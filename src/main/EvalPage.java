import com.google.gson.stream.JsonWriter;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.Select;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// https://course-evals.utoronto.ca/BPI/fbview.aspx?blockid=wq5ZlPATMIwLMyWevu
// block ID determines which faculty is being used
public class EvalPage {

    private final String courseValuesId = "divFbvDetails";

    private final WebDriver driver;
    private final String saveLocation;
    private final Serializer serializer;
    private final String departmentValuesId = "divFbvSubjectsValues";

    public EvalPage(WebDriver driver, String saveLocation) {
        this.driver = driver;
        this.saveLocation = saveLocation;
        this.serializer = new Serializer(null);
        String url = "https://course-evals.utoronto.ca/BPI/fbview.aspx?blockid=wq5ZlPATMIwLMyWevu";
        driver.get(url);
        PageFactory.initElements(driver, this);
        waitForLoad();
    }

    private void waitForLoad() {
        try {
            Thread.sleep(2000);
            // no implicit wait unless enabled manually
            int count = 0;
            while (count < 120) {
                String loadingDialogId = "waitMachineID";
                if (driver.findElements(By.id(loadingDialogId)).size() == 0) return;
                Thread.sleep(1000);
                count++;
            }
            throw new RuntimeException("Page took over 1 minute to fetch data");
        } catch (InterruptedException ignored) {

        }
    }

    private void waitForInputLoad() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException ignored) {

        }
    }

    private WebElement findById(String id) {
        return driver.findElement(By.id(id));
    }

    private WebElement findByXpath(String xpath) {
        return driver.findElement(By.xpath(xpath));
    }

    public List<String> getDepartmentNames() {
        waitForLoad();
        return findById(departmentValuesId).findElements(By.tagName("p")).stream().filter(p -> !p.getAttribute("id").contains("_")).map(p -> p.getAttribute("textContent")).collect(Collectors.toList());
    }

    private void initializeDepartment(String deptName) {
        waitForLoad();

        String departmentInputId = "txtFbvSubjectsValues";
        findById(departmentInputId).clear();
        findById(departmentInputId).sendKeys(deptName);
        waitForInputLoad();

        selectDepartmentFromList(deptName);
        waitForLoad();
    }

    private void selectDepartmentFromList(String deptName) {
        for (WebElement p : findById(departmentValuesId).findElements(By.tagName("p"))) {
            if (p.getAttribute("textContent").equals(deptName)) {
                p.click();
                return;
            }
        }
        throw new RuntimeException("Failed to find department in dropdown.");
    }

    public void fetchDepartmentData(String deptName) throws IOException {
        List<String> courseCodes = new ArrayList<>();

        initializeDepartment(deptName);

        findById(courseValuesId).findElements(By.tagName("p")).forEach(p -> {
            if (!p.getAttribute("id").contains("_")) {
                courseCodes.add(p.getAttribute("id"));
            }
        });
        for (String code : courseCodes) {
            saveCourseData(deptName, code);
        }
    }

    public void saveCourseData(String deptName, String courseCode) throws IOException {
        System.out.println("Obtaining data from " + courseCode);
        String courseInputId = "txtFbvDetails";

        String itemsPerPageXPath = "//*[@id=\"fbvGridPageSizeSelectBlock\"]/select";
        new Select(findByXpath(itemsPerPageXPath)).selectByValue("100");
        waitForLoad();

        boolean succeeded = false;
        List<Header> header = null;
        List<Map<String, String>> rows = null;
        while (!succeeded) {
            try {
                findById(courseInputId).click();
                findById(courseValuesId).findElements(By.tagName("p")).stream().filter(p -> p.getAttribute("id").equals(courseCode)).forEach(WebElement::click);

                waitForLoad();

                header = getTableHeaders();
                rows = getTableContents();
                succeeded = true;
            } catch (NoSuchElementException e) {
                if (driver.getPageSource().contains("500")) {
                    initializeDepartment(deptName);
                }
            }
        }

        LocalDate timestamp = LocalDate.now();
        String fileName = courseCode.toLowerCase() + "-" + timestamp.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".json";
        String path = saveLocation + "\\" + fileName;
        serializer.setWriter(new JsonWriter(new BufferedWriter(new FileWriter(path))));
        serializer.serializeCourse(header, rows);
    }

    private List<Header> getTableHeaders() {
        List<Header> output = new ArrayList<>();
        String headerClass = "affix-table-wrapper";
        driver.findElement(By.className(headerClass)).findElement(By.tagName("tr")).findElements(By.tagName("th")).forEach(h -> {
            String title = h.getText().replaceAll("[^a-zA-Z0-9\\s]+", "").trim();
            String description = null;
            if (h.findElements(By.className("tooltip")).size() > 0 && !title.replaceAll("Item [1-6]", "").equals("")) {
                description = h.findElement(By.className("tooltip-content")).getAttribute("textContent");
            }
            output.add(new Header(title, description));
        });
        return output;
    }

    private List<Map<String, String>> getTableContents() {
        int remainingEntries = Integer.parseInt(findById("fbvGridNbItemsTotalLvl1").getText().replace("Total ", ""));
        List<Map<String, String>> contents = new ArrayList<>();
        String tableId = "fbvGrid";

        while (true) {
            remainingEntries -= 100;
            findById(tableId).findElements(By.className("gData")).forEach(tr -> contents.add(getRowContents(tr)));
            if (remainingEntries < 0) break;
            pressNextThenWaitForLoad();
        }
        return contents;
    }

    private Map<String, String> getRowContents(WebElement row) {
        Map<String, String> values = new HashMap<>();
        List<WebElement> columns = row.findElements(By.tagName("td"));
        IntStream.range(0, columns.size()).forEach(i -> values.put(String.valueOf(i), columns.get(i).getText()));
        return values;
    }

    private void pressNextThenWaitForLoad() {
        List<WebElement> inputs = findById("fbvGridSearchBarLvl1").findElements(By.tagName("input"));
        for (WebElement i : inputs) {
            if (i.getAttribute("value").equals(">")) {
                i.click();
                break;
            }
        }
        waitForLoad();
    }

    public static class Header {
        public String title;
        public String description;

        public Header(String title, String description) {
            this.title = title;
            this.description = description;
        }
    }
}

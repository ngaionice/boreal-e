import com.google.gson.stream.JsonWriter;
import org.openqa.selenium.*;
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


    private final String firstInputId = "txtFbvSubjects";
    private final String firstValuesId = "divFbvSubjects";
    private final String firstToggleId = "imgFbvSubjects";

    private final String secondInputId = "txtFbvSubjectsValues";
    private final String secondValuesId = "divFbvSubjectsValues";
    private final String secondToggleId = "imgFbvSubjectsValues";

    private final String thirdValuesId = "divFbvDetails";
    private final String thirdToggleId = "imgFbvDetails";

    private final String itemsPerPageSelectXPath = "//*[@id=\"fbvGridPageSizeSelectBlock\"]/select";

    private final WebDriver driver;
    private final String saveLocation;
    private final Serializer serializer;


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

    public void switchToCourseCodeMode() {
        findById("imgFbvSubjects").click();
        waitForInputLoad();
        findById("divFbvSubjects").findElements(By.tagName("p")).get(1).click();
        waitForLoad();
    }

    /**
     * Assumes using Department mode.
     */
    public List<String> getAllCourseCodes() {
        String getMoreId = "getNextFbvDetails";
        findById(thirdToggleId).click();
        waitForInputLoad();

        while (driver.findElements(By.id(getMoreId)).size() > 0) {
            findById(getMoreId).click();
            waitForLoad();
            waitForInputLoad();
        }
        return findById(thirdValuesId).findElements(By.tagName("p")).stream().filter(p -> !p.getAttribute("id").contains("_")).map(p -> p.getAttribute("textContent")).collect(Collectors.toList());
    }

    private WebElement findById(String id) {
        return driver.findElement(By.id(id));
    }

    private WebElement findByXpath(String xpath) {
        return driver.findElement(By.xpath(xpath));
    }

    public List<String> getDepartmentNames() {
        waitForLoad();
        return findById(secondValuesId).findElements(By.tagName("p")).stream().filter(p -> !p.getAttribute("id").contains("_")).map(p -> p.getAttribute("textContent")).collect(Collectors.toList());
    }

    private void initializeDepartment(String deptName) {
        waitForLoad();

        boolean selectedCorrectDept = false;
        do {
            findById(secondInputId).clear();
            findById(secondInputId).sendKeys(deptName);
            waitForInputLoad();
            // the first row should be the target dept, if not then retry
            WebElement deptRow = findById(secondValuesId).findElement(By.tagName("p"));
            try {
                if (deptRow.getAttribute("textContent").equals(deptName)) {
                    deptRow.click();
                }
                selectedCorrectDept = true;
            } catch (StaleElementReferenceException ignored) {
            }
        } while (!selectedCorrectDept);

        waitForLoad();
    }

    public void fetchDepartmentData(String deptName) throws IOException {
        List<String> courseCodes = new ArrayList<>();

        initializeDepartment(deptName);

        findById(thirdValuesId).findElements(By.tagName("p")).forEach(p -> {
            if (!p.getAttribute("id").contains("_")) {
                courseCodes.add(p.getAttribute("id"));
            }
        });
        for (String code : courseCodes) {
            saveCourseData(deptName, code);
        }
    }


    private void writeCourseDataToFile(String courseCode, List<Header> header, List<Map<String, String>> rows) throws IOException {
        LocalDate timestamp = LocalDate.now();
        String fileName = courseCode.toLowerCase() + "-" + timestamp.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".json";
        String path = saveLocation + "\\" + fileName;
        serializer.setWriter(new JsonWriter(new BufferedWriter(new FileWriter(path))));
        serializer.serializeCourse(header, rows);
    }

    private void refreshPage() {
        driver.get(driver.getCurrentUrl());
        PageFactory.initElements(driver, this);
        waitForLoad();
    }

    private void setItemsPerPage() {
        new Select(findByXpath(itemsPerPageSelectXPath)).selectByValue("100");
        waitForLoad();
    }

    public void saveCourseData(String courseCode) throws IOException {

        setItemsPerPage();

        boolean succeeded = false;
        List<Header> header = null;
        List<Map<String, String>> rows = null;
        int attempts = 0;
        while (!succeeded && attempts < 30) {
            try {
                findById(secondInputId).clear();
                waitForInputLoad();
                findById(secondInputId).sendKeys(courseCode);
                waitForInputLoad();
                findById(secondValuesId).findElements(By.tagName("p")).stream().filter(p -> p.getAttribute("id").equals(courseCode)).forEach(WebElement::click);

                waitForLoad();

                header = getTableHeaders();
                rows = getTableContents();
                succeeded = true;
            } catch (NoSuchElementException | StaleElementReferenceException e) {
                refreshPage();
                switchToCourseCodeMode();
                setItemsPerPage();
                attempts++;
            }
        }
        writeCourseDataToFile(courseCode, header, rows);
    }

    public void saveCourseData(String deptName, String courseCode) throws IOException {
//        System.out.println("Obtaining data from " + courseCode);
        String courseInputId = "txtFbvDetails";

        setItemsPerPage();

        boolean succeeded = false;
        List<Header> header = null;
        List<Map<String, String>> rows = null;
        while (!succeeded) {
            try {
                findById(courseInputId).click();
                findById(thirdValuesId).findElements(By.tagName("p")).stream().filter(p -> p.getAttribute("id").equals(courseCode)).forEach(WebElement::click);

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

        writeCourseDataToFile(courseCode, header, rows);
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

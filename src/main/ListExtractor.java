import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.IOException;
import java.util.List;

public class ListExtractor implements Runnable {

    // Fetches data for every course in the department in a linear fashion.

    private final List<String> codes;
    private final int index;

    public ListExtractor(List<String> codes, int groupIndex) {
        this.codes = codes;
        this.index = groupIndex;
    }

    @Override
    public void run() {
        System.out.println("Started fetching data for group " + index + " of size " + codes.size() + ": starting with " + codes.get(0) + ".");
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless", "--window-size=1920,1080");
        WebDriver driver = new ChromeDriver(options);
        try {
            EvalPage evalPage = new EvalPage(driver, "D:\\projects\\IdeaProjects\\boreal-e\\extracted");
            evalPage.switchToCourseCodeMode();
            for (int i = 0; i < codes.size(); i++) {
                String code = codes.get(i);
                System.out.printf("Group %d - %d/%d: fetching data for %s.\n", index, i, codes.size(), code);
                evalPage.saveCourseData(code);
            }
            System.out.println("Finished fetching data for for group " + index + ".");
        } catch (IOException e) {
            throw new RuntimeException("Failed to save some of the data for group " + index + ".");
        } finally {
            driver.close();
            driver.quit();
        }
    }
}

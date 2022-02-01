import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.IOException;

@Deprecated
public class DepartmentExtractor implements Runnable {

    // Fetches data for every course in the department in a linear fashion.

    private final String deptName;

    public DepartmentExtractor(String deptName) {
        this.deptName = deptName;
    }

    @Override
    public void run() {
        System.out.println("Started fetching data for: " + deptName);
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless", "--window-size=1920,1080");
        WebDriver driver = new ChromeDriver(options);
        try {
            EvalPage evalPage = new EvalPage(driver, "D:\\projects\\IdeaProjects\\boreal-e\\extracted");
            evalPage.fetchDepartmentData(deptName);
            System.out.println("Finished fetching data for " + deptName);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save some of the data for " + deptName + ".");
        } finally {
            driver.close();
            driver.quit();
        }
    }
}

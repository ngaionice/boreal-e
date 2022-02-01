import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;


public class Manager {

    public EvalPage evalPage;
    private ThreadPoolExecutor executor;

    public static void main(String[] args) {
        WebDriverManager.chromedriver().setup();
        System.setProperty("webdriver.chrome.silentOutput", "true");
        java.util.logging.Logger.getLogger("org.openqa.selenium").setLevel(Level.OFF);

        Manager main = new Manager();
        WebDriver driver = main.initialize();
        System.out.println("Fetching course codes.");
        List<String> codes = main.evalPage.getAllCourseCodes();
        System.out.println("Fetching data for " + codes.size() + " courses.");
        main.executeCodes(codes);
        driver.quit();
    }

    private WebDriver initialize() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless", "--window-size=1920,1080");
        WebDriver driver = new ChromeDriver(options);

        evalPage = new EvalPage(driver, "D:\\projects\\IdeaProjects\\boreal-e\\extracted");
        executor = new Executor(21, 21, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        return driver;
    }

    @Deprecated
    private void executeDept(List<String> deptNames) {
        deptNames.forEach(d -> executor.execute(new DepartmentExtractor(d)));
        executor.shutdown();
    }

    private void executeCodes(List<String> courseCodes) {
        int size = courseCodes.size() / 20;
        int max = courseCodes.size();
        for (int i = 0; i < 21; i++) {
            executor.execute(new ListExtractor(courseCodes.subList(i * size, Math.min((i + 1) * size, max)), i));
        }
    }
}

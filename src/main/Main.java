import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.IOException;


public class Main {

    private WebDriver driver;
    public EvalPage evalPage;

    public static void main(String[] args) throws IOException {
        Main main = new Main();
        main.initialize();
        main.evalPage.getDepartmentNames().forEach(System.out::println);
        main.evalPage.fetchDepartmentData("Computer Science");
    }

    private void initialize() {
        WebDriverManager.chromedriver().setup();
        driver = new ChromeDriver();
        driver.manage().window().maximize();

        evalPage = new EvalPage(driver, "D:\\projects\\IdeaProjects\\boreal-e\\extracted");
    }
}

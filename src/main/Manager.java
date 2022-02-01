import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.IntStream;


public class Manager {

    public EvalPage evalPage;
    private ThreadPoolExecutor executor;

    public static void main(String[] args) {
        WebDriverManager.chromedriver().setup();
        System.setProperty("webdriver.chrome.silentOutput", "true");
        java.util.logging.Logger.getLogger("org.openqa.selenium").setLevel(Level.OFF);

        // uncomment below to save all eval data
//        saveEvalData("D:\\projects\\IdeaProjects\\boreal-e\\evals");

        String[] deptCodes = "CSUS,ANA,ANT,ARCLA,FAR,AST,BCH,CITA,CSB,CHM,CINE,CLAS,COL,CSC,OISUT,ASI,CRIM,DTS,DRAMA,ES,EAS,EEB,ECO,ENG,ENT,ENVMT,ETHIC,CERES,FRE,GGR,GER,HIS,IHPST,HMB,IMM,ASABS,IRE,INNIS,ITA,JSP,LMP,LIN,MAT,MST,MEDGM,MUSIC,NMC,NEW,NUSCI,GLAF,PCL,PHL,PHY,PSL,POL,PSY,RLG,COMPG,SDST,SLA,SWK,SOC,SAS,SPA,SMC,STAT,TRIN,UC,VIC,WGSI,WDW".split(",");
        String[] sessionCodes = {"20169", "20175", "20179", "20185", "20189", "20195", "20199", "20205", "20209", "20215", "20219"};

        // uncomment below to save dept data for the sessions specified above.
//        saveTimetableData("D:\\projects\\IdeaProjects\\boreal-e\\timetables", deptCodes, sessionCodes);
    }

    private static void saveEvalData(String saveFolderLocation) {
        Manager main = new Manager();
        WebDriver driver = main.initializeEvalFetching(saveFolderLocation);
        System.out.println("Fetching course codes.");
        List<String> codes = main.evalPage.getAllCourseCodes();
        driver.quit();

        System.out.println("Fetching data for " + codes.size() + " courses.");
        main.executeCodes(codes);
        main.executor.shutdown();
    }

    private static void saveTimetableData(String saveFolderLocation, String[] deptCodes, String[] sessionCodes) {
        TimetableDeptDataExtractor extractor = new TimetableDeptDataExtractor();
        IntStream.range(0, sessionCodes.length).forEach(i -> {
            System.out.printf("Session %d/%d - begin fetching data for session %s.\n", i + 1, sessionCodes.length ,sessionCodes[i]);
            IntStream.range(0, deptCodes.length).forEach(j -> {
                System.out.printf("Dept %d/%d - %s\n", j + 1, deptCodes.length, deptCodes[j]);
                extractor.fetchAndSave(deptCodes[j], sessionCodes[i], saveFolderLocation);
            });
        });
    }

    private WebDriver initializeEvalFetching(String saveFolderLocation) {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless", "--window-size=1920,1080");
        WebDriver driver = new ChromeDriver(options);

        evalPage = new EvalPage(driver, saveFolderLocation);
        executor = new Executor(21, 21, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        return driver;
    }

    private void executeCodes(List<String> courseCodes) {
        int size = courseCodes.size() / 20;
        int max = courseCodes.size();
        for (int i = 0; i < 21; i++) {
            executor.execute(new ListExtractor(courseCodes.subList(i * size, Math.min((i + 1) * size, max)), i));
        }
    }
}

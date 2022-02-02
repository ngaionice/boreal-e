import com.google.gson.stream.JsonWriter;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class TimetableDeptDataExtractor {

    private final WebDriver driver;
    private final Serializer serializer;

    public TimetableDeptDataExtractor() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless", "--window-size=1920,1080");
        this.driver = new ChromeDriver(options);
        this.serializer = new Serializer(null);
    }

    private void setSerializerSavePath(String sessionCode, String deptCode, String saveLocation) throws IOException {
        LocalDate timestamp = LocalDate.now();
        String fileName = deptCode.toLowerCase() + "-" + sessionCode + "-" + timestamp.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".json";
        String path = saveLocation + "\\" + fileName;
        serializer.setWriter(new JsonWriter(new BufferedWriter(new FileWriter(path))));
    }

    public void fetchAndSave(String deptCode, String sessionCode, String saveLocation)  {
        try {
            setSerializerSavePath(sessionCode, deptCode, saveLocation);
            driver.get("https://timetable.iit.artsci.utoronto.ca/api/"+ sessionCode +"/courses?org=" + deptCode);
            Thread.sleep(2000);
            String data = driver.findElement(By.tagName("pre")).getAttribute("textContent");
            serializer.serializeTimetableData(data.equals("[]") ? "{}" : data);
        } catch (IOException e) {
            System.out.printf("An error occurred while trying to save the data of %s-%s.\n", sessionCode, deptCode);
        } catch (InterruptedException e) {
            System.out.println("Thread was interrupted while waiting for page to load.");
        }
    }
}

import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.*;
import org.openqa.selenium.support.ui.*;

import java.time.Duration;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AddAssignmentSystemTest {
    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeEach
    public void setup() {
        System.setProperty("webdriver.chrome.driver", "C:\\Users\\samye\\OneDrive\\Desktop\\chromedriver.exe");

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*"); // Prevents origin-related issues
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        driver.get("http://localhost:5173/");
        driver.manage().window().maximize();
    }

    @Test
    public void testInstructorAddsAssignment() {
        // Step 1: Login
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("email"))).sendKeys("ted@csumb.edu");
        driver.findElement(By.id("password")).sendKeys("password");
        driver.findElement(By.id("loginButton")).click();

        // Step 2: Select CST499 section
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//button[text()='CST499']"))).click();

        // Step 3: Click "Add Assignment"
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[text()='Add Assignment']"))).click();

        // Step 4: Fill in form and save
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("title"))).sendKeys("System Test Assignment");
        driver.findElement(By.name("dueDate")).sendKeys("2025-08-20");
        driver.findElement(By.xpath("//button[text()='Save']")).click();

        // Step 5: Verify assignment was added
        WebElement added = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//td[contains(text(), 'System Test Assignment')]")
        ));

        Assertions.assertNotNull(added, "Assignment should be visible in the table.");
    }

    @AfterEach
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}

package com.cst438.controller;

import java.util.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class studentEnrollSystemTest {

  static final String CHROME_DRIVER_FILE_LOCATION = "/Users/ka_l/Desktop/CST438/chromedriver-mac-arm64/chromedriver";
  static final String URL = "http://localhost:5173"; // frontend dev server (replace if 5173 or other)
  static final String STUDENT_EMAIL = "sama@csumb.edu"; // or "sama@..." depending on your auth
  static final String STUDENT_PASSWORD = "sama2025"; // adjust if different


  WebDriver driver;
  Wait<WebDriver> wait;
  static final int DELAY = 2000;

  Random random = new Random();

  @BeforeEach
  public void setUpDriver() {
    System.setProperty("webdriver.chrome.driver", CHROME_DRIVER_FILE_LOCATION);
    ChromeOptions opts = new ChromeOptions();
    opts.addArguments("--remote-allow-origins=*"); // as seen in examples
    driver = new ChromeDriver(opts);
    driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(2));
    wait = new WebDriverWait(driver, Duration.ofSeconds(DELAY));
    driver.get(URL);
    driver.manage().window().maximize();
  }

  @AfterEach
  public void tearDown() {
    if (driver != null) driver.quit();
  }

  private void doLogin(String email, String password) {
    // Navigate to login if not already there
    // Adjust selectors to match your app
    WebElement emailInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("email")));
    emailInput.clear();
    emailInput.sendKeys(email);

    WebElement passwordInput = driver.findElement(By.id("password"));
    passwordInput.clear();
    passwordInput.sendKeys(password);

    driver.findElement(By.id("loginButton")).click();
  }

  private void handleReactConfirmIfPresent() {
    // The app might use a custom modal (react-confirm-alert) instead of window.alert()
    // Try to click the "Yes" or confirmation button if visible.
    try {
      WebElement yesBtn = wait.until(ExpectedConditions.visibilityOfElementLocated(
          By.xpath("//div[contains(@class,'react-confirm-alert-button-group')]//button[.='Yes' or .='Confirm' or .='OK']")));
      yesBtn.click();
    } catch (Exception e) {
      // fallback: maybe it's a native alert
      try {
        Alert alert = wait.until(ExpectedConditions.alertIsPresent());
        alert.accept();
      } catch (Exception ignored) {
      }
    }
  }

  @Test
  public void testStudentDropAndReEnrollCST599AndVerifyTranscript() throws InterruptedException {
    // 1. Login as student sama
    doLogin(STUDENT_EMAIL, STUDENT_PASSWORD);
    // optionally wait for some indicator of successful login (e.g., schedule nav showing)
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("nav-schedule"))); // replace with real nav id

    // 2. View class schedule for Fall 2025
    driver.findElement(By.id("nav-schedule")).click(); // adjust to actual schedule button/link
    // set term/year if required
    // Example: select Fall and 2025; adjust input/select IDs accordingly
    WebElement termSelect = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("term")));
    termSelect.clear();
    termSelect.sendKeys("Fall"); // if a dropdown, use Select wrapper instead
    WebElement yearInput = driver.findElement(By.id("year"));
    yearInput.clear();
    yearInput.sendKeys("2025");
    driver.findElement(By.id("viewSchedule")).click(); // placeholder button to refresh schedule

    // 3. Drop enrollment for CST599 if present
    // Locate row for CST599 and click "Drop"
    try {
      WebElement dropRow = wait.until(ExpectedConditions.visibilityOfElementLocated(
          By.xpath("//tr[./td/text()='CST599']//button[contains(.,'Drop')]")));
      dropRow.click();
      // handle confirmation (react-confirm or alert)
      handleReactConfirmIfPresent();
      // optionally wait for some success indicator (e.g., toast or table refresh)
      Thread.sleep(1000);
    } catch (Exception e) {
      // If not found, maybe it wasn't enrolled yet; proceed
    }

    // 4. Navigate to enroll page
    driver.findElement(By.id("nav-enroll")).click(); // adjust ID for enrollment page
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("courseCode"))); // placeholder

    // 5. Select CST599 and enroll for Fall 2025
    // Fill term/year if needed here again
    WebElement courseInput = driver.findElement(By.id("courseCode")); // e.g., input or select
    courseInput.clear();
    courseInput.sendKeys("CST599");
    // If term/year needed again on this page:
    WebElement termEnroll = driver.findElement(By.id("termEnroll"));
    termEnroll.clear();
    termEnroll.sendKeys("Fall");
    WebElement yearEnroll = driver.findElement(By.id("yearEnroll"));
    yearEnroll.clear();
    yearEnroll.sendKeys("2025");

    driver.findElement(By.id("enrollButton")).click(); // actual enroll action

    // Wait for enrollment to succeed (e.g., schedule updates)
    // Could check that CST599 now appears in schedule
    wait.until(ExpectedConditions.visibilityOfElementLocated(
        By.xpath("//tr[./td/text()='CST599']")));

    // 6. Navigate to transcript
    driver.findElement(By.id("nav-transcript")).click(); // adjust
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("transcriptTable")));

    // 7. Verify that CST599 is listed with no grade
    WebElement transcriptRow = driver.findElement(
        By.xpath("//tr[./td/text()='CST599']"));
    assertNotNull(transcriptRow, "CST599 should appear in transcript after enrollment");

    // Assume grade cell has a class or position; adjust accordingly
    WebElement gradeCell = transcriptRow.findElement(By.xpath("./td[contains(@class,'grade') or position()=3]")); // tweak to actual
    String gradeText = gradeCell.getText().trim();

    // Accept common no-grade representations: empty, "-", "TBD"
    boolean noGrade = gradeText.isEmpty() || gradeText.equals("-") || gradeText.equalsIgnoreCase("TBD");
    assertTrue(noGrade, "Expected no grade for CST599 but found: '" + gradeText + "'");
  }
}


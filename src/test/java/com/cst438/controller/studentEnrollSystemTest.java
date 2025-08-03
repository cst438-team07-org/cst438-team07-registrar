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

  // Slow mode configuration
  private static final boolean SLOW_MO = true;             // toggle to false for fast CI runs
  private static final long SLOW_DELAY_MS = 1200;          // milliseconds per pause

  WebDriver driver;
  WebDriverWait wait;

  @BeforeEach
  public void setUpDriver() {
    System.setProperty("webdriver.chrome.driver", CHROME_DRIVER_FILE_LOCATION);
    ChromeOptions opts = new ChromeOptions();
    opts.addArguments("--remote-allow-origins=*");
    driver = new ChromeDriver(opts);
    driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(2));
    wait = new WebDriverWait(driver, Duration.ofSeconds(20));
    driver.get(URL);
    driver.manage().window().maximize();
  }

  @AfterEach
  public void tearDown() {
    if (driver != null) driver.quit();
  }

  private void slow() {
    if (SLOW_MO) {
      try {
        Thread.sleep(SLOW_DELAY_MS);
      } catch (InterruptedException ignored) {}
    }
  }

  private void doLogin(String email, String password) {
    WebElement emailInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("email")));
    emailInput.clear();
    emailInput.sendKeys(email);

    WebElement passwordInput = driver.findElement(By.id("password"));
    passwordInput.clear();
    passwordInput.sendKeys(password);

    driver.findElement(By.id("loginButton")).click();
    slow();

    // Wait for a post-login indicator (schedule/transcript links or username)
    wait.until(ExpectedConditions.or(
        ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[contains(.,'Schedule')]")),
        ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[contains(.,'Transcript')]"))
    ));
    slow();
  }

  private void handleReactConfirmIfPresent() {
    try {
      WebElement yesBtn = wait.until(ExpectedConditions.visibilityOfElementLocated(
          By.xpath("//div[contains(@class,'react-confirm-alert-button-group')]//button[.='Yes' or .='Confirm' or .='OK']")));
      yesBtn.click();
      slow();
    } catch (Exception e) {
      try {
        Alert alert = wait.until(ExpectedConditions.alertIsPresent());
        alert.accept();
        slow();
      } catch (Exception ignored) {}
    }
  }

  @Test
  public void testStudentDropAndReEnrollCST599AndVerifyTranscript() throws InterruptedException {
    // 1. Login as sama
    doLogin(STUDENT_EMAIL, STUDENT_PASSWORD);

    // 2. Navigate to Schedule view
    WebElement scheduleNav = wait.until(ExpectedConditions.elementToBeClickable(
        By.xpath("//a[contains(., 'Schedule') or contains(., 'My Class Schedule')]")));
    scheduleNav.click();
    slow();

    // 3. Select Fall 2025 term and get schedule (if inputs exist)
    try {
      WebElement yearInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
          By.xpath("//input[@name='year' or @placeholder='Year']")));
      yearInput.clear();
      yearInput.sendKeys("2025");
      slow();
      WebElement semesterInput = driver.findElement(By.xpath("//input[@name='semester' or @placeholder='Semester']"));
      semesterInput.clear();
      semesterInput.sendKeys("Fall");
      slow();
      WebElement getSchedule = driver.findElement(By.xpath("//button[contains(.,'Get Schedule')]"));
      getSchedule.click();
      slow();
    } catch (Exception ignored) {
      // If term selection is implicit, proceed
    }

    // Wait for schedule heading/table
    wait.until(ExpectedConditions.visibilityOfElementLocated(
        By.xpath("//h3[contains(.,'My Class Schedule')]")));
    slow();

    // 4. Drop CST599 if present
    try {
      WebElement cst599Row = wait.until(ExpectedConditions.visibilityOfElementLocated(
          By.xpath("//tr[.//td[contains(.,'cst599')]]")));
      slow();
      WebElement dropBtn = cst599Row.findElement(By.xpath(".//button[contains(.,'Drop')]"));
      dropBtn.click();
      slow();
      handleReactConfirmIfPresent();
      slow();
    } catch (Exception ignore) {
      // not enrolled yet, that's fine
    }

    // 5. Navigate to enrollment / open sections page
    WebElement enrollNav = wait.until(ExpectedConditions.elementToBeClickable(
        By.xpath("//a[contains(., 'Enroll') or contains(., 'Open Sections')]")));
    enrollNav.click();
    slow();

    // 6. Select Fall 2025 to fetch open sections if applicable
    try {
      WebElement yearInputEnroll = wait.until(ExpectedConditions.visibilityOfElementLocated(
          By.xpath("//input[@name='year' or @placeholder='Year']")));
      yearInputEnroll.clear();
      yearInputEnroll.sendKeys("2025");
      slow();
      WebElement semesterInputEnroll = driver.findElement(By.xpath("//input[@name='semester' or @placeholder='Semester']"));
      semesterInputEnroll.clear();
      semesterInputEnroll.sendKeys("Fall");
      slow();
      WebElement getSections = driver.findElement(By.xpath("//button[contains(.,'Get Sections')]"));
      getSections.click();
      slow();
    } catch (Exception ignored) {
      // term selection might be auto-handled
    }

    // 7. Find CST599 in open sections and click Add
    WebElement cst599OpenRow = wait.until(ExpectedConditions.visibilityOfElementLocated(
        By.xpath("//tr[.//td[contains(.,'cst599')]]")));
    slow();
    WebElement addBtn = cst599OpenRow.findElement(By.xpath(".//button[contains(.,'Add')]"));
    addBtn.click();
    slow();
    handleReactConfirmIfPresent();
    slow();

    // 8. Navigate to Transcript
    WebElement transcriptNav = wait.until(ExpectedConditions.elementToBeClickable(
        By.xpath("//a[contains(., 'Transcript')]")));
    transcriptNav.click();
    slow();

    // 9. Verify CST599 appears with no grade
    wait.until(ExpectedConditions.visibilityOfElementLocated(
        By.xpath("//h3[contains(.,'Transcript')]")));
    slow();
    WebElement transcriptRow = wait.until(ExpectedConditions.visibilityOfElementLocated(
        By.xpath("//tr[.//td[contains(.,'cst599')]]")));
    assertNotNull(transcriptRow, "CST599 should appear in transcript after enrollment");
    slow();

    WebElement gradeCell = transcriptRow.findElement(By.xpath("./td[last()]"));
    String gradeText = gradeCell.getText().trim();
    boolean noGrade = gradeText.isEmpty() || gradeText.equals("-") || gradeText.equalsIgnoreCase("TBD") || gradeText.equalsIgnoreCase("null");
    assertTrue(noGrade, "Expected no grade for CST599 but found: '" + gradeText + "'");
    slow();
  }
}
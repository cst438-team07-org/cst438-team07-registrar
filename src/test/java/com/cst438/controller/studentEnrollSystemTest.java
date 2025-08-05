package com.cst438.controller;

import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
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

@TestMethodOrder(OrderAnnotation.class)
public class studentEnrollSystemTest {

  static final String CHROME_DRIVER_FILE_LOCATION = "/Users/ka_l/Desktop/CST438/chromedriver-mac-arm64/chromedriver";
  static final String URL = "http://localhost:5173";
 // static final String STUDENT_EMAIL = "sam1@csumb.edu";
 // static final String STUDENT_PASSWORD = "sam2025";

  // Slow mode configuration
  private static final boolean SLOW_MO = true;
  private static final long SLOW_DELAY_MS = 1200;

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
    if (driver != null)
      driver.quit();
  }

  private void slow() {
    if (SLOW_MO) {
      try {
        Thread.sleep(SLOW_DELAY_MS);
      } catch (InterruptedException ignored) {
      }
    }
  }

  private void doLogin(String email, String password) {
    WebElement e = wait.until(
        ExpectedConditions.visibilityOfElementLocated(By.id("email")));
    e.clear();
    e.sendKeys(email);
    WebElement p = driver.findElement(By.id("password"));
    p.clear();
    p.sendKeys(password);
    driver.findElement(By.id("loginButton")).click();
    slow();
    // this guarantees past the login page:
    wait.until(ExpectedConditions.visibilityOfElementLocated(
        By.xpath("//a[contains(text(),'Logout')]")));
    slow();
  }


  private void handleReactConfirmIfPresent() {
    try {
      WebElement yesBtn = wait.until(ExpectedConditions.visibilityOfElementLocated(
          By.xpath(
              "//div[contains(@class,'react-confirm-alert-button-group')]//button[.='Yes' or .='Confirm' or .='OK']")));
      yesBtn.click();
      slow();
    } catch (Exception e) {
      try {
        Alert alert = wait.until(ExpectedConditions.alertIsPresent());
        alert.accept();
        slow();
      } catch (Exception ignored) {
      }
    }
  }

  private void doInstructorLogin(String email, String password) {
    doLogin(email, password);
  }

  @Order(1)
  @Test
  public void testStudentDropAndReEnrollCST599AndVerifyTranscript() throws InterruptedException {
    // Login as sama
    doLogin("sam1@csumb.edu", "sam2025");

    // Navigate to Schedule view
    WebElement scheduleNav = wait.until(ExpectedConditions.elementToBeClickable(
        By.xpath("//a[contains(., 'Schedule') or contains(., 'My Class Schedule')]")));
    scheduleNav.click();
    slow();

    // Select Fall 2025 term and get schedule (if inputs exist)
    try {
      WebElement yearInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
          By.xpath("//input[@name='year' or @placeholder='Year']")));
      yearInput.clear();
      yearInput.sendKeys("2025");
      slow();
      WebElement semesterInput = driver.findElement(
          By.xpath("//input[@name='semester' or @placeholder='Semester']"));
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

    // Drop CST599 if present
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
    }

    // Navigate to enrollment / open sections page
    WebElement enrollNav = wait.until(ExpectedConditions.elementToBeClickable(
        By.xpath("//a[contains(., 'Enroll') or contains(., 'Open Sections')]")));
    enrollNav.click();
    slow();

    // Select Fall 2025 to fetch open sections if applicable
    try {
      WebElement yearInputEnroll = wait.until(ExpectedConditions.visibilityOfElementLocated(
          By.xpath("//input[@name='year' or @placeholder='Year']")));
      yearInputEnroll.clear();
      yearInputEnroll.sendKeys("2025");
      slow();
      WebElement semesterInputEnroll = driver.findElement(
          By.xpath("//input[@name='semester' or @placeholder='Semester']"));
      semesterInputEnroll.clear();
      semesterInputEnroll.sendKeys("Fall");
      slow();
      WebElement getSections = driver.findElement(By.xpath("//button[contains(.,'Get Sections')]"));
      getSections.click();
      slow();
    } catch (Exception ignored) {
     }

    // Find CST599 in open sections and click Add
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

    //  Verify CST599 appears with no grade
    wait.until(ExpectedConditions.visibilityOfElementLocated(
        By.xpath("//h3[contains(.,'Transcript')]")));
    slow();
    WebElement transcriptRow = wait.until(ExpectedConditions.visibilityOfElementLocated(
        By.xpath("//tr[.//td[contains(.,'cst599')]]")));
    assertNotNull(transcriptRow, "CST599 should appear in transcript after enrollment");
    slow();

    WebElement gradeCell = transcriptRow.findElement(By.xpath("./td[last()]"));
    String gradeText = gradeCell.getText().trim();
    boolean noGrade =
        gradeText.isEmpty() || gradeText.equals("-") || gradeText.equalsIgnoreCase("TBD")
            || gradeText.equalsIgnoreCase("null");
    assertTrue(noGrade, "Expected no grade for CST599 but found: '" + gradeText + "'");
    slow();
  }


  @Order(2)
  @Test
  public void testInstructorViewCST599Roster() throws InterruptedException {
    // Login as instructor
    doInstructorLogin("ted@csumb.edu", "ted2025");

    // Select Fall 2025 term and get sections
    WebElement yearInput = wait.until(ExpectedConditions.elementToBeClickable(
        By.xpath("//input[@name='year' or @placeholder='Year']")));
    yearInput.clear(); yearInput.sendKeys("2025"); slow();
    WebElement semesterInput = driver.findElement(
        By.xpath("//input[@name='semester' or @placeholder='Semester']"));
    semesterInput.clear(); semesterInput.sendKeys("Fall"); slow();
    driver.findElement(By.xpath("//button[contains(.,'Get Sections')]")).click(); slow();

    // Wait for Sections heading
    wait.until(ExpectedConditions.visibilityOfElementLocated(
        By.xpath("//h3[contains(.,'Sections')]")
    )); slow();

    // Find CST599 row and click its Enrollments link
    WebElement row = wait.until(ExpectedConditions.visibilityOfElementLocated(
        By.xpath("//tr[.//td[contains(text(),'cst599')]]")
    )); slow();

    // click the Link
    row.findElement(By.id("enrollmentsLink")).click(); slow();

    // Verify Sama appears once
    wait.until(ExpectedConditions.visibilityOfElementLocated(
        By.xpath("//h3[contains(.,'Enrollments')]")
    )); slow();
    List<WebElement> entries = driver.findElements(
        By.xpath("//td[contains(text(),'sam1@csumb.edu')]")
    );
    assertEquals(1, entries.size(),
        "Expected exactly one sama entry in roster"); slow();
  }
}

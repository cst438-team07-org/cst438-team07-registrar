package com.cst438.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Random;

public class InstructorAddAssignmentAndGradeSystemTest {

  static final String CHROME_DRIVER_FILE_LOCATION =
      "/Users/ka_l/Desktop/CST438/chromedriver-mac-arm64/chromedriver";
  static final String URL = "http://localhost:5173";

  private WebDriver driver;
  private WebDriverWait wait;
  private static final long SLOW_MS = 800;

  @BeforeEach
  public void setUp() {
    System.setProperty("webdriver.chrome.driver", CHROME_DRIVER_FILE_LOCATION);
    ChromeOptions opts = new ChromeOptions();
    opts.addArguments("--remote-allow-origins=*");
    driver = new ChromeDriver(opts);

    driver.manage().timeouts().implicitlyWait(java.time.Duration.ZERO);
    wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(20));

    driver.get(URL);
    driver.manage().window().maximize();
  }

  @AfterEach
  public void tearDown() {
    if (driver != null) driver.quit();
  }

  private void slow() {
    try { Thread.sleep(SLOW_MS); }
    catch (InterruptedException ignored) { }
  }

  private void doLogin(String email, String password) {
    // wait for email field and pause before interacting
    WebElement e = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("email")));
    slow();                        // pause before clearing
    e.clear();
    slow();                        // pause before typing
    e.sendKeys(email);
    slow();                        // pause after typing email

    // now move to password field
    WebElement p = driver.findElement(By.id("password"));
    slow();                        // before clearing
    p.clear();
    slow();                        // before typing
    p.sendKeys(password);
    slow();                        // after typing password

    // click login
    driver.findElement(By.id("loginButton")).click();
    slow();                        // pause after clicking

    // wait for logout link to confirm login and then pause
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("logoutLink")));
    slow();
  }


  @Test
  public void testInstructorAddAssignmentAndGradeCST599() {
    // 1) Login
    doLogin("ted@csumb.edu", "ted2025");

    // 2) Select Fall 2025
    WebElement year = wait.until(ExpectedConditions.elementToBeClickable(
        By.xpath("//input[@name='year' or @placeholder='Year']")));
    year.clear(); year.sendKeys("2025"); slow();
    WebElement sem = driver.findElement(
        By.xpath("//input[@name='semester' or @placeholder='Semester']"));
    sem.clear(); sem.sendKeys("Fall"); slow();
    driver.findElement(By.xpath("//button[contains(text(),'Get Sections')]")).click();
    slow();

    // 3) Navigate to CST599 → Assignments
    WebElement row = wait.until(ExpectedConditions.visibilityOfElementLocated(
        By.xpath("//tr[.//td[contains(text(),'cst599')]]")));
    slow();
    row.findElement(By.xpath(".//a[contains(text(),'Assignments')]")).click();
    slow();
    wait.until(ExpectedConditions.visibilityOfElementLocated(
        By.xpath("//h2[contains(text(),'Assignments for')]")));
    slow();

    // 4) Click "Add Assignment"
    driver.findElement(By.id("addAssignmentButton")).click();
    WebElement addDialog = wait.until(ExpectedConditions.visibilityOfElementLocated(
        By.cssSelector("dialog[open]")));
    slow();

    // 5) Pick a random due date in [2025-08-20 .. 2025-12-17]
    LocalDate start = LocalDate.of(2025, 8, 20);
    LocalDate end   = LocalDate.of(2025, 12, 17);
    long span = ChronoUnit.DAYS.between(start, end);
    LocalDate randomDate = start.plusDays(new Random().nextInt((int)span + 1));
    // Format as MM/dd/yyyy for native picker
    String localDateStr = randomDate.format(
        DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.ENGLISH));

    // 6) Fill title & dueDate inside the open dialog
    String title = "assignment" + new Random().nextInt(100_000);
    addDialog.findElement(By.cssSelector("input[name='title']"))
        .sendKeys(title);
    slow();
    WebElement dateInput = addDialog.findElement(By.cssSelector("input[name='dueDate']"));
    dateInput.clear();
    dateInput.sendKeys(localDateStr);
    slow();

    // 7) Save then Close the dialog so assignments refresh
    addDialog.findElement(By.xpath(".//button[text()='Save']")).click();
    slow();
    addDialog.findElement(By.xpath(".//button[text()='Close']")).click();
    wait.until(ExpectedConditions.invisibilityOf(addDialog));
    slow();

    // 8) After close, fetch the assignments list and click the new one
    WebElement newRow = wait.until(ExpectedConditions.visibilityOfElementLocated(
        By.xpath("//tr[.//td[text()='" + title + "']]")));
    assertNotNull(newRow, "New assignment should appear: " + title);
    slow();

    // 9) Open grading dialog for that assignment
    newRow.findElement(By.cssSelector("button#gradeButton")).click();
    WebElement gradeDialog = wait.until(ExpectedConditions.visibilityOfElementLocated(
        By.cssSelector("dialog[open]")));
    slow();

    // 10) Enter scores 60, 88, 98
    String[] students = { "sama", "samb", "samc" };
    String[] scores   = { "60", "88", "98" };
    for (int i = 0; i < students.length; i++) {
      WebElement in = gradeDialog.findElement(By.xpath(
          ".//td[contains(text(),'" + students[i] + "')]/following-sibling::td//input[@name='score']"));
      in.clear();
      in.sendKeys(scores[i]);
      slow();
    }

    // 11) Save then Close grade dialog
    gradeDialog.findElement(By.xpath(".//button[text()='Save']")).click();
    slow();
    gradeDialog.findElement(By.xpath(".//button[text()='Close']")).click();
    wait.until(ExpectedConditions.invisibilityOf(gradeDialog));
    slow();

    // 12) Re-open to verify scores persisted
    newRow.findElement(By.cssSelector("button#gradeButton")).click();
    gradeDialog = wait.until(ExpectedConditions.visibilityOfElementLocated(
        By.cssSelector("dialog[open]")));
    slow();
    for (int i = 0; i < students.length; i++) {
      String actual = gradeDialog.findElement(By.xpath(
              ".//td[contains(text(),'" + students[i] + "')]/following-sibling::td//input[@name='score']"))
          .getAttribute("value");
      assertEquals(scores[i], actual, students[i] + " score");
    }
    slow();

    // 13) Close final grading dialog
    gradeDialog.findElement(By.xpath(".//button[text()='Close']")).click();
  }
}
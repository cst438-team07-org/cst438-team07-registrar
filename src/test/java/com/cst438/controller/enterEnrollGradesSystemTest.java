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

public class enterEnrollGradesSystemTest {

  static final String CHROME_DRIVER_FILE_LOCATION = "C:/chromedriver_win64/chromedriver.exe";
  static final String URL = "http://localhost:5173";
  static final int DELAY = 2000; // 2 seconds
  static final String INSTRUCTOR_EMAIL = "ted@csumb.edu";
  static final String INSTRUCTOR_PASSWORD = "ted2025";
  static final String STUDENT_EMAIL = "sam2@csumb.edu";
  static final String STUDENT_PASSWORD = "sam2025";
  WebDriver driver;
  Wait<WebDriver> wait;

  @BeforeEach
  public void setUpDriver() {
    System.setProperty("webdriver.chrome.driver", CHROME_DRIVER_FILE_LOCATION);
    ChromeOptions ops = new ChromeOptions();
    ops.addArguments("--remote-allow-origins=*");
    driver = new ChromeDriver(ops);
    driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(2));
    wait = new WebDriverWait(driver, Duration.ofSeconds(2));
    driver.get(URL);
    driver.manage().window().maximize();
  }

  @AfterEach
  public void quit() {
        driver.quit();
    }

  @Test
  public void testEnterEnrollGrades() throws InterruptedException {
    Alert alert;
    //Instructor ted@csumb.edu logins. 
    Thread.sleep(DELAY);
    driver.findElement(By.id("email")).sendKeys(INSTRUCTOR_EMAIL);
    driver.findElement(By.id("password")).sendKeys(INSTRUCTOR_PASSWORD);
    Thread.sleep(DELAY);
    driver.findElement(By.id("loginButton")).click();
    Thread.sleep(DELAY);
    
    // On the home page for instructor enter 2025 and Fall to view the list of sections.  
    driver.findElement(By.id("year")).sendKeys("2025");
    driver.findElement(By.id("semester")).sendKeys("Fall");
    driver.findElement(By.id("selectTermButton")).click();
    Thread.sleep(DELAY);

    // Click on enrollments for CST599.
    driver.findElement(By.xpath("//td[contains(text(),'cst599')]/following-sibling::td/a[contains(text(),'Enrollments')]")).click();
    Thread.sleep(DELAY);
      
    // Clear any existing grade and enter final grades of  A, B+ and C for the sama, samb and samc.  
    driver.findElement(By.xpath("//td[contains(text(),'sama')]/following-sibling::td/input")).clear();
    driver.findElement(By.xpath("//td[contains(text(),'sama')]/following-sibling::td/input")).sendKeys("A");
    driver.findElement(By.xpath("//td[contains(text(),'samb')]/following-sibling::td/input")).clear();
    driver.findElement(By.xpath("//td[contains(text(),'samb')]/following-sibling::td/input")).sendKeys("B+");
    driver.findElement(By.xpath("//td[contains(text(),'samc')]/following-sibling::td/input")).clear();
    driver.findElement(By.xpath("//td[contains(text(),'samc')]/following-sibling::td/input")).sendKeys("C");

    // Save the grades and close the dialog.  
    driver.findElement(By.xpath("//button[contains(text(),'Save')]")).click();
    Thread.sleep(DELAY);

    // View the class roster again and verify the grades.  
    driver.findElement(By.id("homeLink")).click();
    Thread.sleep(DELAY);
    driver.findElement(By.id("year")).sendKeys("2025");
    driver.findElement(By.id("semester")).sendKeys("Fall");
    driver.findElement(By.id("selectTermButton")).click();
    Thread.sleep(DELAY);

    WebElement we = driver.findElement(By.xpath("//tr[./td[text() = 'cst599']]"));
    assertNotNull(we, "CST599 should appear in the schedule");
    we.findElement(By.xpath(".//a[contains(text(),'Enrollments')]")).click();
    Thread.sleep(DELAY);

    // Verify the grade input values for sama, samb, and samc
    String samaGrade = driver.findElement(By.xpath("//td[contains(text(),'sama')]/following-sibling::td/input")).getAttribute("value");
    assertEquals("A", samaGrade, "sama should have grade A");

    String sambGrade = driver.findElement(By.xpath("//td[contains(text(),'samb')]/following-sibling::td/input")).getAttribute("value");
    assertEquals("B+", sambGrade, "samb should have grade B+");

    String samcGrade = driver.findElement(By.xpath("//td[contains(text(),'samc')]/following-sibling::td/input")).getAttribute("value");
    assertEquals("C", samcGrade, "samc should have grade C");
    Thread.sleep(DELAY);

    // Logout. 
    driver.findElement(By.id("logoutLink")).click();
    we = null;
    Thread.sleep(DELAY);

    //Login as student samb.
    driver.findElement(By.id("email")).sendKeys(STUDENT_EMAIL);
    driver.findElement(By.id("password")).sendKeys(STUDENT_PASSWORD);
    Thread.sleep(DELAY);
    driver.findElement(By.id("loginButton")).click();
    Thread.sleep(DELAY);

    // Navigate to view transcript and verify that cst599 with a grade of B+ is listed.
    driver.findElement(By.partialLinkText("Transcript")).click();
    Thread.sleep(DELAY);

    // Assert that anywhere on the page says samb.
    WebElement studentNameElement = driver.findElement(By.xpath("//div[contains(text(), 'Student name :')]"));

    // Get the text of the element
    String studentNameText = studentNameElement.getText();
    System.out.println(studentNameText);

    // Assert that 'samb' is in the student name (case-insensitive)
    assertTrue(studentNameText.toLowerCase().contains("samb"), String.format("Expected 'samb' in student name, but got '%s'", studentNameText));
    
    // Verify that the grade B+ appears in the transcript for samb.
    we = null;
    we = driver.findElement(By.xpath("//td[contains(text(),'cst599')]/following-sibling::td[contains(text(),'B+')]"));
    assertNotNull(we, "CST599 should appear in transcript with grade B+");
  }
}
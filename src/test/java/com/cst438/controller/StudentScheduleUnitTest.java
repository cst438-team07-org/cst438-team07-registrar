package com.cst438.controller;

import com.cst438.domain.*;
import com.cst438.dto.EnrollmentDTO;
import com.cst438.dto.LoginDTO;
import com.cst438.service.GradebookServiceProxy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class StudentScheduleUnitTest {

    @Autowired
    private WebTestClient client;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TermRepository termRepository;

    @Autowired
    private CourseRepository courseRepository;

    @MockitoBean
    GradebookServiceProxy gradebookService;

    Random random = new Random();

    @Test
    public void testAddAndDropEnrollment() throws Exception {
        // Login as student and get the security token
        String studentEmail = "sam@csumb.edu";
        String password = "sam2025";

        EntityExchangeResult<LoginDTO> loginResult = client.get().uri("/login")
                .headers(headers -> headers.setBasicAuth(studentEmail, password))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginDTO.class).returnResult();

        String jwt = loginResult.getResponseBody().jwt();
        assertNotNull(jwt);

        // Retrieve existing student and instructor
        User student = userRepository.findByEmail(studentEmail);
        assertNotNull(student, "Student user sam@csumb.edu not found in database");
        assertEquals("STUDENT", student.getType(), "User sam@csumb.edu is not a STUDENT");

        User instructor = userRepository.findByEmail("ted@csumb.edu");
        assertNotNull(instructor, "Instructor user ted@csumb.edu not found in database");
        assertEquals("INSTRUCTOR", instructor.getType(), "User ted@csumb.edu is not an INSTRUCTOR");

        // Create test data
        Term term = new Term();
        term.setYear(2025);
        term.setSemester("Fall");
        term.setAddDate(Date.valueOf(LocalDate.now().minusDays(1))); // Yesterday
        term.setAddDeadline(Date.valueOf(LocalDate.now().plusDays(10))); // In 10 days
        term.setDropDeadline(Date.valueOf(LocalDate.now().plusDays(15))); // In 15 days
        term.setStartDate(Date.valueOf(LocalDate.now().minusDays(15))); // 15 days ago
        term.setEndDate(Date.valueOf(LocalDate.now().plusDays(30))); // In 30 days
        termRepository.save(term);

        Course course = new Course();
        course.setCourseId("cst363");
        course.setTitle("Database Management");
        course.setCredits(3);
        courseRepository.save(course);

        Section section = new Section();
        section.setSectionId(1);
        section.setCourse(course);
        section.setTerm(term);
        section.setBuilding("090");
        section.setRoom("E410");
        section.setTimes("T Th 9-10");
        section.setInstructorEmail(instructor.getEmail());
        sectionRepository.save(section);

        // Test adding an enrollment (Good Path)
        EntityExchangeResult<EnrollmentDTO> enrollmentResult = client.post()
                .uri("/enrollments/sections/" + section.getSectionNo())
                .headers(headers -> headers.setBearerAuth(jwt))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(EnrollmentDTO.class).returnResult();

        EnrollmentDTO actualEnrollment = enrollmentResult.getResponseBody();
        assertTrue(actualEnrollment.enrollmentId() > 0, "Enrollment ID is invalid");

        // Verify enrollment in database
        Enrollment enrollment = enrollmentRepository.findById(actualEnrollment.enrollmentId()).orElse(null);
        assertNotNull(enrollment, "Enrollment not found in database");
        assertEquals(student.getId(), enrollment.getStudent().getId(), "Student ID mismatch");
        assertEquals(section.getSectionNo(), enrollment.getSection().getSectionNo(), "Section number mismatch");

        // Verify gradebook service call
        verify(gradebookService, times(1)).sendMessage(eq("addEnrollment"), any(EnrollmentDTO.class));

        // Test dropping the enrollment (Good Path)
        client.delete()
                .uri("/enrollments/" + actualEnrollment.enrollmentId())
                .headers(headers -> headers.setBearerAuth(jwt))
                .exchange()
                .expectStatus().isOk();

        // Verify enrollment was deleted
        enrollment = enrollmentRepository.findById(actualEnrollment.enrollmentId()).orElse(null);
        assertNull(enrollment, "Enrollment was not deleted from database");

        // Verify gradebook service call
        verify(gradebookService, times(1)).sendMessage(eq("deleteEnrollment"), any());

        // Cleanup
        sectionRepository.delete(section);
        termRepository.delete(term);
        courseRepository.delete(course);
    }

    @Test
    public void testAddEnrollmentInvalidSection() throws Exception {
        // Login as student and get the security token
        String studentEmail = "sam@csumb.edu";
        String password = "sam2025";

        EntityExchangeResult<LoginDTO> loginResult = client.get().uri("/login")
                .headers(headers -> headers.setBasicAuth(studentEmail, password))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginDTO.class).returnResult();

        String jwt = loginResult.getResponseBody().jwt();
        assertNotNull(jwt);

        // Attempt to enroll in a non-existent section (Bad Path)
        int invalidSectionNo = random.nextInt(10000);
        client.post()
                .uri("/enrollments/sections/" + invalidSectionNo)
                .headers(headers -> headers.setBearerAuth(jwt))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.errors[0]").isEqualTo("Section not found");

        // Verify no gradebook interaction
        verify(gradebookService, times(0)).sendMessage(anyString(), any());
    }

    @Test
    public void testAddEnrollmentDuplicate() throws Exception {
        // Login as student and get the security token
        String studentEmail = "sam@csumb.edu";
        String password = "sam2025";

        EntityExchangeResult<LoginDTO> loginResult = client.get().uri("/login")
                .headers(headers -> headers.setBasicAuth(studentEmail, password))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginDTO.class).returnResult();

        String jwt = loginResult.getResponseBody().jwt();
        assertNotNull(jwt);

        // Retrieve existing student and instructor
        User student = userRepository.findByEmail(studentEmail);
        assertNotNull(student, "Student user sam@csumb.edu not found in database");
        assertEquals("STUDENT", student.getType(), "User sam@csumb.edu is not a STUDENT");

        User instructor = userRepository.findByEmail("ted@csumb.edu");
        assertNotNull(instructor, "Instructor user ted@csumb.edu not found in database");
        assertEquals("INSTRUCTOR", instructor.getType(), "User ted@csumb.edu is not an INSTRUCTOR");

        // Create test data
        Term term = new Term();
        term.setYear(2025);
        term.setSemester("Fall");
        term.setAddDate(Date.valueOf(LocalDate.now().minusDays(1)));
        term.setAddDeadline(Date.valueOf(LocalDate.now().plusDays(10)));
        term.setDropDeadline(Date.valueOf(LocalDate.now().plusDays(15)));
        term.setStartDate(Date.valueOf(LocalDate.now().minusDays(15)));
        term.setEndDate(Date.valueOf(LocalDate.now().plusDays(30)));
        termRepository.save(term);

        Course course = new Course();
        course.setCourseId("cst363");
        course.setTitle("Database Management");
        course.setCredits(3);
        courseRepository.save(course);

        Section section = new Section();
        section.setSectionId(1);
        section.setCourse(course);
        section.setTerm(term);
        section.setBuilding("090");
        section.setRoom("E410");
        section.setTimes("T Th 9-10");
        section.setInstructorEmail(instructor.getEmail());
        sectionRepository.save(section);

        // Create existing enrollment
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setSection(section);
        enrollment.setGrade(null);
        enrollmentRepository.save(enrollment);

        // Attempt to enroll in the same section (Bad Path)
        client.post()
                .uri("/enrollments/sections/" + section.getSectionNo())
                .headers(headers -> headers.setBearerAuth(jwt))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.errors[0]").isEqualTo("Student already enrolled in this section");

        // Verify no gradebook interaction
        verify(gradebookService, times(0)).sendMessage(anyString(), any());

        // Cleanup
        enrollmentRepository.delete(enrollment);
        sectionRepository.delete(section);
        termRepository.delete(term);
        courseRepository.delete(course);
    }

    @Test
    public void testAddEnrollmentOutsideAddPeriod() throws Exception {
        // Login as student and get the security token
        String studentEmail = "sam@csumb.edu";
        String password = "sam2025";

        EntityExchangeResult<LoginDTO> loginResult = client.get().uri("/login")
                .headers(headers -> headers.setBasicAuth(studentEmail, password))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginDTO.class).returnResult();

        String jwt = loginResult.getResponseBody().jwt();
        assertNotNull(jwt);

        // Retrieve existing instructor
        User instructor = userRepository.findByEmail("ted@csumb.edu");
        assertNotNull(instructor, "Instructor user ted@csumb.edu not found in database");
        assertEquals("INSTRUCTOR", instructor.getType(), "User ted@csumb.edu is not an INSTRUCTOR");

        // Create test data with closed add period
        Term term = new Term();
        term.setYear(2025);
        term.setSemester("Fall");
        term.setAddDate(Date.valueOf(LocalDate.now().minusDays(10)));
        term.setAddDeadline(Date.valueOf(LocalDate.now().minusDays(1)));
        term.setDropDeadline(Date.valueOf(LocalDate.now().plusDays(5)));
        term.setStartDate(Date.valueOf(LocalDate.now().minusDays(15)));
        term.setEndDate(Date.valueOf(LocalDate.now().plusDays(30)));
        termRepository.save(term);

        Course course = new Course();
        course.setCourseId("cst363");
        course.setTitle("Database Management");
        course.setCredits(3);
        courseRepository.save(course);

        Section section = new Section();
        section.setSectionId(1);
        section.setCourse(course);
        section.setTerm(term);
        section.setBuilding("090");
        section.setRoom("E410");
        section.setTimes("T Th 9-10");
        section.setInstructorEmail(instructor.getEmail());
        sectionRepository.save(section);

        // Attempt to enroll outside add period (Bad Path)
        client.post()
                .uri("/enrollments/sections/" + section.getSectionNo())
                .headers(headers -> headers.setBearerAuth(jwt))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.errors[0]").isEqualTo("Enrollment period is closed");

        // Verify no gradebook interaction
        verify(gradebookService, times(0)).sendMessage(anyString(), any());

        // Cleanup
        sectionRepository.delete(section);
        termRepository.delete(term);
        courseRepository.delete(course);
    }

    @Test
    public void testDropEnrollmentInvalidEnrollment() throws Exception {
        // Login as student and get the security token
        String studentEmail = "sam@csumb.edu";
        String password = "sam2025";

        EntityExchangeResult<LoginDTO> loginResult = client.get().uri("/login")
                .headers(headers -> headers.setBasicAuth(studentEmail, password))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginDTO.class).returnResult();

        String jwt = loginResult.getResponseBody().jwt();
        assertNotNull(jwt);

        // Attempt to drop a non-existent enrollment (Bad Path)
        int invalidEnrollmentId = random.nextInt(10000);
        client.delete()
                .uri("/enrollments/" + invalidEnrollmentId)
                .headers(headers -> headers.setBearerAuth(jwt))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.errors[0]").isEqualTo("Enrollment not found");

        // Verify no gradebook interaction
        verify(gradebookService, times(0)).sendMessage(anyString(), any());
    }

    @Test
    public void testDropEnrollmentWrongStudent() throws Exception {
        // Login as student and get the security token
        String studentEmail = "sam@csumb.edu";
        String password = "sam2025";

        EntityExchangeResult<LoginDTO> loginResult = client.get().uri("/login")
                .headers(headers -> headers.setBasicAuth(studentEmail, password))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginDTO.class).returnResult();

        String jwt = loginResult.getResponseBody().jwt();
        assertNotNull(jwt);

        // Retrieve existing instructor and other student
        User instructor = userRepository.findByEmail("ted@csumb.edu");
        assertNotNull(instructor, "Instructor user ted@csumb.edu not found in database");
        assertEquals("INSTRUCTOR", instructor.getType(), "User ted@csumb.edu is not an INSTRUCTOR");

        User otherStudent = userRepository.findByEmail("admin@csumb.edu");
        assertNotNull(otherStudent, "User admin@csumb.edu not found in database");

        // Create test data
        Term term = new Term();
        term.setYear(2025);
        term.setSemester("Fall");
        term.setAddDate(Date.valueOf(LocalDate.now().minusDays(1)));
        term.setAddDeadline(Date.valueOf(LocalDate.now().plusDays(10)));
        term.setDropDeadline(Date.valueOf(LocalDate.now().plusDays(15)));
        term.setStartDate(Date.valueOf(LocalDate.now().minusDays(15)));
        term.setEndDate(Date.valueOf(LocalDate.now().plusDays(30)));
        termRepository.save(term);

        Course course = new Course();
        course.setCourseId("cst363");
        course.setTitle("Database Management");
        course.setCredits(3);
        courseRepository.save(course);

        Section section = new Section();
        section.setSectionId(1);
        section.setCourse(course);
        section.setTerm(term);
        section.setBuilding("090");
        section.setRoom("E410");
        section.setTimes("T Th 9-10");
        section.setInstructorEmail(instructor.getEmail());
        sectionRepository.save(section);

        // Create enrollment for another student (using admin@csumb.edu as a different user)
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(otherStudent);
        enrollment.setSection(section);
        enrollment.setGrade(null);
        enrollmentRepository.save(enrollment);

        // Attempt to drop another student's enrollment (Bad Path)
        client.delete()
                .uri("/enrollments/" + enrollment.getEnrollmentId())
                .headers(headers -> headers.setBearerAuth(jwt))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.errors[0]").isEqualTo("Enrollment does not belong to the student");

        // Verify no gradebook interaction
        verify(gradebookService, times(0)).sendMessage(anyString(), any());

        // Cleanup
        enrollmentRepository.delete(enrollment);
        sectionRepository.delete(section);
        termRepository.delete(term);
        courseRepository.delete(course);
    }

    @Test
    public void testDropEnrollmentOutsideDropPeriod() throws Exception {
        // Login as student and get the security token
        String studentEmail = "sam@csumb.edu";
        String password = "sam2025";

        EntityExchangeResult<LoginDTO> loginResult = client.get().uri("/login")
                .headers(headers -> headers.setBasicAuth(studentEmail, password))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginDTO.class).returnResult();

        String jwt = loginResult.getResponseBody().jwt();
        assertNotNull(jwt);

        // Retrieve existing student and instructor
        User student = userRepository.findByEmail(studentEmail);
        assertNotNull(student, "Student user sam@csumb.edu not found in database");
        assertEquals("STUDENT", student.getType(), "User sam@csumb.edu is not a STUDENT");

        User instructor = userRepository.findByEmail("ted@csumb.edu");
        assertNotNull(instructor, "Instructor user ted@csumb.edu not found in database");
        assertEquals("INSTRUCTOR", instructor.getType(), "User ted@csumb.edu is not an INSTRUCTOR");

        // Create test data with closed drop period
        Term term = new Term();
        term.setYear(2025);
        term.setSemester("Fall");
        term.setAddDate(Date.valueOf(LocalDate.now().minusDays(10)));
        term.setAddDeadline(Date.valueOf(LocalDate.now().plusDays(5)));
        term.setDropDeadline(Date.valueOf(LocalDate.now().minusDays(1)));
        term.setStartDate(Date.valueOf(LocalDate.now().minusDays(15)));
        term.setEndDate(Date.valueOf(LocalDate.now().plusDays(30)));
        termRepository.save(term);

        Course course = new Course();
        course.setCourseId("cst363");
        course.setTitle("Database Management");
        course.setCredits(3);
        courseRepository.save(course);

        Section section = new Section();
        section.setSectionId(1);
        section.setCourse(course);
        section.setTerm(term);
        section.setBuilding("090");
        section.setRoom("E410");
        section.setTimes("T Th 9-10");
        section.setInstructorEmail(instructor.getEmail());
        sectionRepository.save(section);

        // Create test enrollment
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setSection(section);
        enrollment.setGrade(null);
        enrollmentRepository.save(enrollment);

        // Attempt to drop after drop deadline (Bad Path)
        client.delete()
                .uri("/enrollments/" + enrollment.getEnrollmentId())
                .headers(headers -> headers.setBearerAuth(jwt))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.errors[0]").isEqualTo("Drop period has ended");

        // Verify no gradebook interaction
        verify(gradebookService, times(0)).sendMessage(anyString(), any());

        // Cleanup
        enrollmentRepository.delete(enrollment);
        sectionRepository.delete(section);
        termRepository.delete(term);
        courseRepository.delete(course);
    }
}
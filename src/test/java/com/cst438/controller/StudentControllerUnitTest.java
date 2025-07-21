package com.cst438.controller;

import com.cst438.domain.Course;
import com.cst438.domain.Enrollment;
import com.cst438.domain.EnrollmentRepository;
import com.cst438.domain.Section;
import com.cst438.domain.Term;
import com.cst438.domain.User;
import com.cst438.domain.UserRepository;
import com.cst438.dto.EnrollmentDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.security.Principal;
import java.util.List;

import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentControllerTest {

  @Mock
  EnrollmentRepository enrollmentRepository;

  @Mock
  UserRepository userRepository;

  @Mock
  Principal principal;

  @InjectMocks
  StudentController controller;

  // --- GET /enrollments?year=&semester= ---

  @Test
  void getSchedule_success() {
    int year = 2025;
    String semester = "Fall";
    String email = "stu@school.edu";
    int studentId = 123;

    // 1) stub student lookup
    User user = new User();
    user.setId(studentId);
    user.setName("Alice");
    user.setEmail(email);
    when(userRepository.findByEmail(email)).thenReturn(user);
    when(principal.getName()).thenReturn(email);

    // 2) build Course → Term → Section → Enrollment graph
    Course course = new Course();
    course.setCourseId("CS101");
    course.setTitle("Intro");
    course.setCredits(3);

    Term term = new Term();
    term.setYear(year);
    term.setSemester(semester);

    Section section = new Section();
    section.setSectionId(1);
    section.setSectionNo(10);
    section.setCourse(course);
    section.setTerm(term);
    section.setBuilding("Bldg");
    section.setRoom("101");
    section.setTimes("MWF 9-10");

    Enrollment e = new Enrollment();
    e.setEnrollmentId(55);
    e.setGrade("B+");
    e.setStudent(user);
    e.setSection(section);

    // 3) stub repository
    when(enrollmentRepository
        .findByYearAndSemesterOrderByCourseId(year, semester, studentId))
        .thenReturn(List.of(e));

    // 4) exercise
    List<EnrollmentDTO> dtos = controller.getSchedule(year, semester, principal);

    // 5) verify
    assertNotNull(dtos);
    assertEquals(1, dtos.size());

    EnrollmentDTO dto = dtos.get(0);
    assertEquals(55,        dto.enrollmentId());
    assertEquals("B+",      dto.grade());
    assertEquals(123,       dto.studentId());
    assertEquals("Alice",   dto.name());
    assertEquals(email,     dto.email());
    assertEquals("CS101",   dto.courseId());
    assertEquals("Intro",   dto.title());
    assertEquals(3,         dto.credits());
    assertEquals(1,         dto.sectionId());
    assertEquals(10,        dto.sectionNo());
    assertEquals("Bldg",    dto.building());
    assertEquals("101",     dto.room());
    assertEquals("MWF 9-10",dto.times());
    assertEquals(year,      dto.year());
    assertEquals(semester,  dto.semester());

    verify(enrollmentRepository)
        .findByYearAndSemesterOrderByCourseId(year, semester, studentId);
  }

  @Test
  void getSchedule_studentNotFound_throws404() {
    // stub principal to return some non‐null name
    String missingEmail = "noone@nowhere";
    when(principal.getName()).thenReturn(missingEmail);
    // now stub userRepository for that same value
    when(userRepository.findByEmail(missingEmail)).thenReturn(null);

    ResponseStatusException ex = assertThrows(
        ResponseStatusException.class,
        () -> controller.getSchedule(2025, "Spring", principal)
    );
    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
  }

  // --- GET /transcripts ---

  @Test
  void getTranscript_success() {
    String email = "stu@school.edu";
    int studentId = 456;

    // 1) stub student lookup
    User user = new User();
    user.setId(studentId);
    user.setName("Bob");
    user.setEmail(email);
    when(userRepository.findByEmail(email)).thenReturn(user);
    when(principal.getName()).thenReturn(email);

    // 2) build two Enrollment records
    Course course = new Course();
    course.setCourseId("MATH100");
    course.setTitle("Calculus");
    course.setCredits(4);

    Term term = new Term();
    term.setYear(2024);
    term.setSemester("Spring");

    Section section = new Section();
    section.setSectionId(2);
    section.setSectionNo(20);
    section.setCourse(course);
    section.setTerm(term);
    section.setBuilding("Math Hall");
    section.setRoom("102");
    section.setTimes("TTh 1-2");

    Enrollment e1 = new Enrollment();
    e1.setEnrollmentId(11);
    e1.setGrade("A");
    e1.setStudent(user);
    e1.setSection(section);

    Enrollment e2 = new Enrollment();
    e2.setEnrollmentId(22);
    e2.setGrade("B");
    e2.setStudent(user);
    e2.setSection(section);

    // 3) stub repository
    when(enrollmentRepository.findEnrollmentsByStudentIdOrderByTermId(studentId))
        .thenReturn(List.of(e1, e2));

    // 4) exercise
    List<EnrollmentDTO> dtos = controller.getTranscript(principal);

    // 5) verify
    assertNotNull(dtos);
    assertEquals(2, dtos.size());

    // spot-check first DTO
    EnrollmentDTO dto1 = dtos.get(0);
    assertEquals(11,         dto1.enrollmentId());
    assertEquals("A",        dto1.grade());
    assertEquals("MATH100",  dto1.courseId());
    assertEquals("Calculus",  dto1.title());

    verify(enrollmentRepository)
        .findEnrollmentsByStudentIdOrderByTermId(studentId);
  }

  @Test
  void getTranscript_studentNotFound_throws404() {
    String missingEmail = "absent@none";
    when(principal.getName()).thenReturn(missingEmail);
    when(userRepository.findByEmail(missingEmail)).thenReturn(null);

    ResponseStatusException ex = assertThrows(
        ResponseStatusException.class,
        () -> controller.getTranscript(principal)
    );
    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
  }
}

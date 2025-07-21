package com.cst438.controller;

import com.cst438.domain.*;
import com.cst438.dto.EnrollmentDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.security.Principal;
import java.util.*;

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

    // 1) stub user lookup
    User user = new User();
    user.setId(studentId);
    user.setName("Alice");
    user.setEmail(email);
    when(userRepository.findByEmail(email)).thenReturn(user);
    when(principal.getName()).thenReturn(email);

    // 2) build a real Enrollment → Section → Course → Term graph
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

    when(enrollmentRepository
        .findByYearAndSemesterOrderByCourseId(year, semester, studentId))
        .thenReturn(List.of(e));

    // 3) exercise
    List<EnrollmentDTO> dtos = controller.getSchedule(year, semester, principal);

    // 4) verify
    assertNotNull(dtos);
    assertEquals(1, dtos.size());

    EnrollmentDTO dto = dtos.get(0);
    assertEquals(55,         dto.enrollmentId());
    assertEquals("B+",       dto.grade());
    assertEquals(studentId,  dto.studentId());
    assertEquals("Alice",    dto.name());
    assertEquals(email,      dto.email());
    assertEquals("CS101",    dto.courseId());
    assertEquals("Intro",    dto.title());
    assertEquals(3,          dto.credits());
    assertEquals(1,          dto.sectionId());
    assertEquals(10,         dto.sectionNo());
    assertEquals("Bldg",     dto.building());
    assertEquals("101",      dto.room());
    assertEquals("MWF 9-10", dto.times());
    assertEquals(year,       dto.year());
    assertEquals(semester,   dto.semester());

    verify(enrollmentRepository)
        .findByYearAndSemesterOrderByCourseId(year, semester, studentId);
  }


  @Test
  void getSchedule_studentNotFound_throws404() {
    when(userRepository.findByEmail(anyString())).thenReturn(null);
    ResponseStatusException ex = assertThrows(
        ResponseStatusException.class,
        () -> controller.getSchedule(2025, "Spring", principal)
    );
    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
  }

  // --- GET /transcripts ---

  @Test
  void getTranscript_success() {
    String studentEmail = "stu@school.edu";
    int studentId = 456;

    User user = new User();
    user.setId(studentId);
    user.setEmail(studentEmail);
    when(userRepository.findByEmail(studentEmail)).thenReturn(user);
    when(principal.getName()).thenReturn(studentEmail);

    Enrollment e1 = mock(Enrollment.class);
    when(enrollmentRepository.findEnrollmentsByStudentIdOrderByTermId(studentId))
        .thenReturn(Arrays.asList(e1, e1));

    List<EnrollmentDTO> dtos = controller.getTranscript(principal);

    assertEquals(2, dtos.size());
  }

  @Test
  void getTranscript_studentNotFound_throws404() {
    when(userRepository.findByEmail(anyString())).thenReturn(null);
    ResponseStatusException ex = assertThrows(
        ResponseStatusException.class,
        () -> controller.getTranscript(principal)
    );
    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
  }
}

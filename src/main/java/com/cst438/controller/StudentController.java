package com.cst438.controller;

import com.cst438.domain.*;
import com.cst438.dto.EnrollmentDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

@RestController
public class StudentController {

   private final EnrollmentRepository enrollmentRepository;
   private final UserRepository userRepository;

   public StudentController(
           EnrollmentRepository enrollmentRepository,
           UserRepository userRepository
   ) {
       this.enrollmentRepository = enrollmentRepository;
       this.userRepository = userRepository;
   }

   // retrieve schedule for student for a term
   @GetMapping("/enrollments")
   @PreAuthorize("hasAuthority('SCOPE_ROLE_STUDENT')")
   public List<EnrollmentDTO> getSchedule(
           @RequestParam("year") int year,
           @RequestParam("semester") String semester,
           Principal principal) {
			   
		// use the EnrollmentController findByYearAndSemsterOrderByCourseId
		// method to retrive the enrollments given the year, semester and id 
		// of the logged in student.
		// Return a list of EnrollmentDTO.
        User student= userRepository.findByEmail(principal.getName()); 
        if (student == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found");
        }
        List<Enrollment> enrollments = enrollmentRepository.findByYearAndSemesterAndStudentIdOrderByCourseId(
                year, semester, student.getId());
        
        List<EnrollmentDTO> result = new ArrayList<>();
        for (Enrollment e : enrollments) {
            result.add(new EnrollmentDTO(
                e.getEnrollmentId(),
                e.getGrade(),
                e.getStudent().getId(),
                e.getStudent().getName(),
                e.getStudent().getEmail(),
                e.getSection().getCourse().getCourseId(),
                e.getSection().getCourse().getTitle(),
                e.getSection().getSectionId(),
                e.getSection().getSectionNo(),
                e.getSection().getBuilding(),
                e.getSection().getRoom(),
                e.getSection().getTimes(),
                e.getSection().getCourse().getCredits(),
                e.getSection().getTerm().getYear(),
                e.getSection().getTerm().getSemester()
            ));
        }
        return result;

   }

   // return transcript for student
    @GetMapping("/transcripts")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_STUDENT')")
    public List<EnrollmentDTO> getTranscript(Principal principal) {

        // use the EnrollmentController findEnrollmentsByStudentIdOrderByTermId
		// method to retrive the enrollments given the id 
		// of the logged in student.
		// Return a list of EnrollmentDTO.
           
        // Check if the student exists
        User student = userRepository.findByEmail(principal.getName());
        if (student == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found");
    }
    // Retrieve enrollments for the student
        List<Enrollment> enrollments = enrollmentRepository.findEnrollmentsByStudentIdOrderByTermId(student.getId());
        
        List<EnrollmentDTO> result = new ArrayList<>();
        for (Enrollment e : enrollments) {
            result.add(new EnrollmentDTO(
                e.getEnrollmentId(),
                e.getGrade(),
                e.getStudent().getId(),
                e.getStudent().getName(),
                e.getStudent().getEmail(),
                e.getSection().getCourse().getCourseId(),
                e.getSection().getCourse().getTitle(),
                e.getSection().getSectionId(),
                e.getSection().getSectionNo(),
                e.getSection().getBuilding(),
                e.getSection().getRoom(),
                e.getSection().getTimes(),
                e.getSection().getCourse().getCredits(),
                e.getSection().getTerm().getYear(),
                e.getSection().getTerm().getSemester()
            ));
        }
        return result;
    }
}
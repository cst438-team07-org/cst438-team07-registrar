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

       User student = userRepository.findByEmail(principal.getName());
        // return list of EnrollmentDTOs with all fields populated
       List<Enrollment> enrollments = enrollmentRepository.findByYearAndSemesterOrderByCourseId(year, semester, student.getId());
       return enrollments.stream().map(e -> new EnrollmentDTO(
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
               )
       ).toList();
   }

   // return transcript for student
    @GetMapping("/transcripts")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_STUDENT')")
    public List<EnrollmentDTO> getTranscript(Principal principal) {

        //return list of EnrollmentDTOs with all fields populated
        User student = userRepository.findByEmail(principal.getName());
        List<Enrollment> enrollments = enrollmentRepository.findEnrollmentsByStudentIdOrderByTermId(student.getId());
        return enrollments.stream().map(e -> new EnrollmentDTO(
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
                )
        ).toList();
    }
}
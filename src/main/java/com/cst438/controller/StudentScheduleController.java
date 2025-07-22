package com.cst438.controller;

import com.cst438.domain.*;
import com.cst438.dto.EnrollmentDTO;
import com.cst438.dto.SectionDTO;
import com.cst438.service.GradebookServiceProxy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RestController
public class StudentScheduleController {

    private final EnrollmentRepository enrollmentRepository;
    private final SectionRepository sectionRepository;
    private final UserRepository userRepository;
    private final GradebookServiceProxy gradebook;

    public StudentScheduleController(
            EnrollmentRepository enrollmentRepository,
            SectionRepository sectionRepository,
            UserRepository userRepository,
            GradebookServiceProxy gradebook
    ) {
        this.enrollmentRepository = enrollmentRepository;
        this.sectionRepository = sectionRepository;
        this.userRepository = userRepository;
        this.gradebook = gradebook;
    }


    @PostMapping("/enrollments/sections/{sectionNo}")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_STUDENT')")
    public EnrollmentDTO addCourse(
            @PathVariable int sectionNo,
            Principal principal ) throws Exception  {
        // Create the Enrollment entity
        Enrollment enrollment = new Enrollment();

        //  relate enrollment to the student's User entity and to the Section entity
        User student = userRepository.findByEmail(principal.getName());
        if (student == null || !student.getType().equals("STUDENT")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid student credentials");
        }
        enrollment.setStudent(student);

        Section section = sectionRepository.findById(sectionNo).orElse(null);
        if (section == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Section not found");
        }
        enrollment.setSection(section);

        //  check that student is not already enrolled in the section
        Enrollment existingEnrollment = enrollmentRepository.findEnrollmentBySectionNoAndStudentId(sectionNo, student.getId());
        if (existingEnrollment != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Student already enrolled in this section");
        }

        //  check that the current date is not before addDate, not after addDeadline
        //  of the section's term.
        Date currentDate = new Date();
        Term term = section.getTerm();
        if (currentDate.before(term.getAddDate()) || currentDate.after(term.getAddDeadline())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enrollment period is closed");
        }

        enrollment.setGrade(null); // Grade is null until assigned by the instructor
        // Save the Enrollment entity
        enrollmentRepository.save(enrollment);

        // Return an EnrollmentDTO with the id of the
        // Enrollment and other fields.
        EnrollmentDTO enrollmentDTO = new EnrollmentDTO(
                enrollment.getEnrollmentId(),
                enrollment.getGrade(),
                student.getId(),
                student.getName(),
                student.getEmail(),
                section.getCourse().getCourseId(),
                section.getCourse().getTitle(),
                section.getSectionId(),
                section.getSectionNo(),
                section.getBuilding(),
                section.getRoom(),
                section.getTimes(),
                section.getCourse().getCredits(),
                term.getYear(),
                term.getSemester()
        );
        gradebook.sendMessage("addEnrollment", enrollmentDTO);


        return enrollmentDTO;
    }

    // student drops a course
    @DeleteMapping("/enrollments/{enrollmentId}")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_STUDENT')")
    public void dropCourse(@PathVariable("enrollmentId") int enrollmentId, Principal principal) throws Exception {
        // Retrieve the logged-in student's User entity
        User student = userRepository.findByEmail(principal.getName());
        if (student == null || !student.getType().equals("STUDENT")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid student credentials");
        }

        // Retrieve the enrollment by enrollmentId
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId).orElse(null);
        if (enrollment == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enrollment not found");
        }

        // check that enrollment belongs to the logged in student
        // and that today is not after the dropDeadLine for the term.
        if (enrollment.getStudent().getId() != student.getId()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enrollment does not belong to the student");
        }

        Date currentDate = new Date();
        Term term = enrollment.getSection().getTerm();
        if (currentDate.after(term.getDropDeadline())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Drop period has ended");
        }

        // Delete the enrollment
        enrollmentRepository.deleteById(enrollmentId);

        // Send message to gradebook service
        gradebook.sendMessage("deleteEnrollment", enrollmentId);

    }

}

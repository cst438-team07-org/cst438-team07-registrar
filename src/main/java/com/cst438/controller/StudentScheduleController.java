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

        // create and save an EnrollmentEntity
        //  relate enrollment to the student's User entity and to the Section entity
        //  check that student is not already enrolled in the section
        //  check that current data is not before addDate, not after addDeadline
        User student = userRepository.findByEmail(principal.getName());
        Section s = sectionRepository.findById(sectionNo).orElse(null);
        if (s==null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "section not found");
        }
        Term t = s.getTerm();
        Enrollment e = enrollmentRepository.findEnrollmentBySectionNoAndStudentId(sectionNo, student.getId());
        if (e!=null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "already enrolled");
        }
        Date now = new Date();
        if (now.after(t.getAddDeadline()) || now.before(t.getAddDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "too early or too late to enroll");
        }

        e = new Enrollment();
        e.setSection(s);
        e.setStudent(student);
        enrollmentRepository.save(e);

        EnrollmentDTO result = new EnrollmentDTO(
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
        );
        gradebook.sendMessage("addEnrollment", result);
        return result;
    }

    // student drops a course
    @DeleteMapping("/enrollments/{enrollmentId}")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_STUDENT')")
    public void dropCourse(@PathVariable("enrollmentId") int enrollmentId, Principal principal) throws Exception {

        // check that enrollment belongs to the logged in student

        Enrollment e = enrollmentRepository.findById(enrollmentId).orElse(null);
        if (e==null || !e.getStudent().getEmail().equals(principal.getName()))  {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid enrollment id");
        }
        // check that today is not after dropDeadline
        Date now = new Date();
        if (now.after(e.getSection().getTerm().getDropDeadline())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cannot drop due to date");
        }
        enrollmentRepository.delete(e);
        gradebook.sendMessage("deleteEnrollment", enrollmentId);
    }

}

package com.cst438.controller2;

import com.cst438.domain.Enrollment;
import com.cst438.domain.EnrollmentRepository;
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

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class StudentTranscriptUnitTest {

    @Autowired
    private WebTestClient client ;


    // a NOOP mock replaces GradebookServiceProxy and RabbitMQ messaging
    @MockitoBean
    GradebookServiceProxy gradebookService;

    String studentLoginJWT;

    @Autowired
    EnrollmentRepository enrollmentRepository;

    /*
      Two students both named "sam" with email id "sam@csumb.edu" and "sam4@csumb.edu"
      login in "sam@csumb.edu" and verify that transcript contains only enrollment for "sam@csumb.edu"
     */
    @Test
    public void getTranscript() {
        login("sam@csumb.edu", "sam2025");
        EntityExchangeResult<EnrollmentDTO[]> schedule = client.get().uri("/transcripts")
                .headers(headers -> headers.setBearerAuth(studentLoginJWT))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(EnrollmentDTO[].class).returnResult();
        assertEquals(3, schedule.getResponseBody().length, "transcript length is incorrect.");
        for (EnrollmentDTO e : schedule.getResponseBody()) {
            Enrollment enrollment = enrollmentRepository.findById(e.enrollmentId()).orElse(null);
            assertEquals("sam@csumb.edu", enrollment.getStudent().getEmail());
        }

        login("sam4@csumb.edu", "sam2025");
        EntityExchangeResult<EnrollmentDTO[]> schedule2 = client.get().uri("/transcripts")
                .headers(headers -> headers.setBearerAuth(studentLoginJWT))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(EnrollmentDTO[].class).returnResult();
        assertEquals(3, schedule2.getResponseBody().length, "transcript length is incorrect.");
        for (EnrollmentDTO e : schedule2.getResponseBody()) {
            Enrollment enrollment = enrollmentRepository.findById(e.enrollmentId()).orElse(null);
            assertEquals("sam4@csumb.edu", enrollment.getStudent().getEmail());
        }
    }

    private void login(String email, String password) {
        EntityExchangeResult<LoginDTO> login_dto =  client.get().uri("/login")
                .headers(headers -> headers.setBasicAuth(email, password))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginDTO.class).returnResult();

        studentLoginJWT = login_dto.getResponseBody().jwt();
        assertNotNull(studentLoginJWT);
    }
}

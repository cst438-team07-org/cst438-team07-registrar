package com.cst438.controller2;

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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class StudentEnrollsOKUnitTest {
    @Autowired
    private WebTestClient client ;

    // a NOOP mock replaces GradebookServiceProxy and RabbitMQ messaging
    @MockitoBean
    GradebookServiceProxy gradebookService;

    String studentLoginJWT;


    // enroll into section 2. Then get student schedule and verify that course is listed
    @Test
    public void enrollOkThenDropOK() {
        login("sam4@csumb.edu", "sam2025");

        // enroll into cst599
        client.post().uri("/enrollments/sections/2")
                .headers(headers -> headers.setBearerAuth(studentLoginJWT))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk();
        // there should still be only 1 message sent from Registrar to Gradebook for the successful enrollment
        verify(gradebookService, times(1)).sendMessage(eq("addEnrollment"), any());

        EntityExchangeResult<EnrollmentDTO[]> schedule = client.get().uri("/enrollments?year=2025&semester=Fall")
                .headers(headers -> headers.setBearerAuth(studentLoginJWT))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(EnrollmentDTO[].class).returnResult();

        int enrollmentId = 0;
        for (EnrollmentDTO e : schedule.getResponseBody()) {
            if (e.courseId().equals("cst599")) {
                enrollmentId = e.enrollmentId();
                break;
            }
        }
        assertNotEquals(0, enrollmentId, "enrolled course cst599 does not appear in schedule");

        // student now drops the course
        client.delete().uri("/enrollments/"+enrollmentId)
                .headers(headers -> headers.setBearerAuth(studentLoginJWT))
                .exchange()
                .expectStatus().isOk();
        verify(gradebookService, times(1)).sendMessage(eq("deleteEnrollment"), any());

        EntityExchangeResult<EnrollmentDTO[]> scheduleUpdated = client.get().uri("/enrollments?year=2025&semester=Fall")
                .headers(headers -> headers.setBearerAuth(studentLoginJWT))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(EnrollmentDTO[].class).returnResult();

        enrollmentId = 0;
        for (EnrollmentDTO e : scheduleUpdated.getResponseBody()) {
            if (e.courseId().equals("cst599")) {
                enrollmentId = e.enrollmentId();
                break;
            }
        }
        assertEquals(0, enrollmentId, "enrolled course cst599 does not appear in schedule");
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

package com.cst438.controller2;

import com.cst438.dto.LoginDTO;
import com.cst438.service.GradebookServiceProxy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class StudentDropCourseFailUnitTest {
    @Autowired
    private WebTestClient client ;


    // a NOOP mock replaces GradebookServiceProxy and RabbitMQ messaging
    @MockitoBean
    GradebookServiceProxy gradebookService;

    String studentLoginJWT;

    // drop invalid enrollment id
    @Test
    public void dropCourseEnrollmentIdNotExists() {
        login("sam@csumb.edu", "sam2025");
        client.delete().uri("/enrollments/999")
                .headers(headers -> headers.setBearerAuth(studentLoginJWT))
                .exchange()
                .expectStatus().is4xxClientError();
        // there is no message sent from Registrar to Gradebook
        verify(gradebookService, times(0)).sendMessage(any(), any());
    }

    // student sam (#2)  attempts to drop section #3 after drop deadline
    @Test
    public void dropCourseAfterDeadlineFails() {
        login("sam@csumb.edu", "sam2025");
        client.delete().uri("/enrollments/3")
                .headers(headers -> headers.setBearerAuth(studentLoginJWT))
                .exchange()
                .expectStatus().is4xxClientError();
        // there is no message sent from Registrar to Gradebook
        verify(gradebookService, times(0)).sendMessage(any(), any());
    }

    // student attempts to drop enrollment that belongs to another student
    @Test
    public void dropCourseForAnother() {
        login("sam@csumb.edu", "sam2025");
        client.delete().uri("/enrollments/3")
                .headers(headers -> headers.setBearerAuth(studentLoginJWT))
                .exchange()
                .expectStatus().is4xxClientError();
        // there is no message sent from Registrar to Gradebook
        verify(gradebookService, times(0)).sendMessage(any(), any());
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

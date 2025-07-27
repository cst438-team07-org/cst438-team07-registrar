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
public class StudentEnrollFailsUnitTest {

    @Autowired
    private WebTestClient client ;

    // a NOOP mock replaces GradebookServiceProxy and RabbitMQ messaging
    @MockitoBean
    GradebookServiceProxy gradebookService;

    String studentLoginJWT;


    // attempt to enroll into invalid section number 99 returns error
    @Test
    public void enrollInvalidSection() {
        login("sam4@csumb.edu", "sam2025");
        client.post().uri("/enrollments/sections/99")
                .headers(headers -> headers.setBearerAuth(studentLoginJWT))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is4xxClientError();
        // there should be no message sent from Registrar to Gradebook for a failed enrollment
        verify(gradebookService, times(0)).sendMessage(any(), any());
    }

    // attempt to enroll into section 3 after add deadline return error
    @Test
    public void enrollAfterDeadline() {
        login("sam4@csumb.edu", "sam2025");
        client.post().uri("/enrollments/sections/3")
                .headers(headers -> headers.setBearerAuth(studentLoginJWT))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is4xxClientError();
        // there should be no message sent from Registrar to Gradebook for a failed enrollment
        verify(gradebookService, times(0)).sendMessage(any(), any());
    }

    // attempt to enroll into section 4 before add date return error
    @Test
    public void enrollBeforeAddDate() {
        login("sam4@csumb.edu", "sam2025");
        client.post().uri("/enrollments/sections/4")
                .headers(headers -> headers.setBearerAuth(studentLoginJWT))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is4xxClientError();
        // there should be no message sent from Registrar to Gradebook for a failed enrollment
        verify(gradebookService, times(0)).sendMessage(any(), any());
    }

    // attempt to enroll into same section twice section 2
    @Test
    public void enrollWhenAlreadyEnrolled() {
        login("sam4@csumb.edu", "sam2025");
        client.post().uri("/enrollments/sections/2")
                .headers(headers -> headers.setBearerAuth(studentLoginJWT))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk();
        // there should be 1 message sent from Registrar to Gradebook for the successful enrollment
        verify(gradebookService, times(1)).sendMessage(eq("addEnrollment"), any());

        client.post().uri("/enrollments/sections/2")
                .headers(headers -> headers.setBearerAuth(studentLoginJWT))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is4xxClientError();
        // there should still be only 1 message sent from Registrar to Gradebook for the successful enrollment
        verify(gradebookService, times(1)).sendMessage(eq("addEnrollment"), any());
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

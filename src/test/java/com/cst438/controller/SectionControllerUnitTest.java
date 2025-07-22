package com.cst438.controller;

import com.cst438.domain.*;
import com.cst438.dto.*;
import com.cst438.service.GradebookServiceProxy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SectionControllerUnitTest {

    @Autowired
    private WebTestClient client ;
    @Autowired
    private SectionRepository sectionRepository;

    // default behavior for a Mock bean
    // return 0 or null for a method that returns a value
    // for method that returns void, the mock method records the call but does nothing
    @MockitoBean
    GradebookServiceProxy gradebookService;
    Random random = new Random();

    @Test
    public void sectionCreateUpdateDelete() throws Exception {

        // login as admin and get the security token
        String adminEmail = "admin@csumb.edu";
        String password = "admin";

        EntityExchangeResult<LoginDTO> login_dto =  client.get().uri("/login")
                .headers(headers -> headers.setBasicAuth(adminEmail, password))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginDTO.class).returnResult();

        String jwt = login_dto.getResponseBody().jwt();
        assertNotNull(jwt);

        // create a new section for course cst363, ted@csumb.edu is the instructor
        // Section information is sent as SectionDTO
        // An updated SectionDTO is returned if create is successful
        SectionDTO sectionDTO = new SectionDTO(
                0, // database will assign the key value
                2025, // year
                "Fall", // semester
                "cst363", // courseId
                null, // title not required on input
                1,   // section id
                "090", // building
                "E410", // room
                "T Th 9-10",  // times
                null, // instructor name is not required on input
                "ted@csumb.edu"  // instructor email must exist in database
        );
        EntityExchangeResult<SectionDTO> sectionResponse =  client.post().uri("/sections")
                .headers(headers -> headers.setBearerAuth(jwt))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(sectionDTO)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(SectionDTO.class).returnResult();
        SectionDTO actualSection = sectionResponse.getResponseBody();
        assertTrue(actualSection.secNo()>0, "primary key value is invalid");

        // check that the sendMessage from registrar to gradebook  was called as expected
        verify(gradebookService, times(1)).sendMessage(eq("addSection"), any());

        // check that the new Section exists in the database
        Section s = sectionRepository.findById(actualSection.secNo()).orElse(null);
        assertNotNull(s, "section created was ok but section not in database");

        // update the section room and times
        sectionDTO = new SectionDTO(
                actualSection.secNo(), // for PUT, primary key is required
                2025,
                "Fall",
                "cst363",
                null, // title is not required on input
                1,   // section id
                "090", // building
                "F110", // updated room
                "T Th 2-3pm",  // updated times
                null, // instructor name is not required on input
                "ted@csumb.edu"  // instructor email must exist in database
        );
        sectionResponse =  client.put().uri("/sections")
                .headers(headers -> headers.setBearerAuth(jwt))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(sectionDTO)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(SectionDTO.class).returnResult();
        actualSection = sectionResponse.getResponseBody();

        // verify change in the database
        s = sectionRepository.findById(actualSection.secNo()).orElse(null);
        assertNotNull(s);
        assertEquals("F110", s.getRoom(), "room change is incorrect");
        assertEquals( "T Th 2-3pm", s.getTimes(), "times change is incorrect");

        // check that the sendMessage was called as expected
        verify(gradebookService, times(1)).sendMessage(eq("updateSection"), any());

        // delete the section
        client.delete().uri("/sections/"+actualSection.secNo())
                .headers(headers -> headers.setBearerAuth(jwt))
                .exchange()
                .expectStatus().isOk();

        s = sectionRepository.findById(actualSection.secNo()).orElse(null);
        assertNull(s, "section was not deleted in database");
        // check that the sendMessage was called as expected
        verify(gradebookService, times(1)).sendMessage(eq("deleteSection"), any());
    }

    @Test
    public void createSectionInvalidInputChar() throws Exception{
        // login as admin and get the security token
        String adminEmail = "admin@csumb.edu";
        String password = "admin";

        EntityExchangeResult<LoginDTO> login_dto =  client.get().uri("/login")
                .headers(headers -> headers.setBasicAuth(adminEmail, password))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginDTO.class).returnResult();

        String jwt = login_dto.getResponseBody().jwt();
        assertNotNull(jwt);

        // create a Section with invalid chars in the building field
        SectionDTO sectionDTO = new SectionDTO(
                0, // database will assign the key value
                2025, // year
                "Fall", // semester
                "cst363", // courseId
                null, // title is not required on input
                1,   // section id
                "<li>90</li>", // building has invalid chars
                "E410", // room
                "T Th 9-10",  // times
                null, // instructor name is not required on input
                "ted@csumb.edu"  // instructor email must exist in database
        );

        // verify the validation error message in the response
         client.post().uri("/sections")
                .headers(headers -> headers.setBearerAuth(jwt))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(sectionDTO)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                // check the list of validation messages
                 .consumeWith(result -> System.out.println(new String(result.getResponseBody())))

                 .jsonPath("$.errors[?(@ =='invalid char in building')]").exists();
    }

    @Test
    public void createSectionInvalidInstructorEmail() throws Exception{
        // login as admin and get the security token
        String adminEmail = "admin@csumb.edu";
        String password = "admin";

        EntityExchangeResult<LoginDTO> login_dto =  client.get().uri("/login")
                .headers(headers -> headers.setBasicAuth(adminEmail, password))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginDTO.class).returnResult();

        String jwt = login_dto.getResponseBody().jwt();
        assertNotNull(jwt);

        // create a new section for course cst363, non existent instructor
        SectionDTO sectionDTO = new SectionDTO(
                0, // database will assign the key value
                2025, // year
                "Fall",  // semester
                "cst363", // courseId
                null, // title is not required on input
                1,   // section id
                "090", // building
                "E410", // room
                "T Th 9-10",  // times
                null, // instructor name is not required on input
                "tedd@csumb.edu"  // invalid instructor email
        );

        // verify the error message from the response
           client.post().uri("/sections")
                .headers(headers -> headers.setBearerAuth(jwt))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(sectionDTO)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                   // extract the error message
                .consumeWith(result -> System.out.println(new String(result.getResponseBody())))
                .jsonPath("$.errors[?(@ =='invalid instructor email')]").exists();
    }
}
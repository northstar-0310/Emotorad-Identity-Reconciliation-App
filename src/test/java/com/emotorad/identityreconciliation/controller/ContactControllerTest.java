package com.emotorad.identityreconciliation.controller;

import com.emotorad.identityreconciliation.model.ContactRequest;
import com.emotorad.identityreconciliation.model.ContactResponse;
import com.emotorad.identityreconciliation.service.ContactService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.hamcrest.Matchers.is;

@WebMvcTest(ContactController.class)
public class ContactControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ContactService contactService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testIdentifyContact() throws Exception {
        // Arrange
        ContactRequest request = new ContactRequest();
        request.setEmail("test@example.com");
        request.setPhoneNumber("1234567890");

        ContactResponse response = new ContactResponse();
        response.setPrimaryContactId(1L);

        // Mock the service response
        when(contactService.identifyContact(any(ContactRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/identify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryContactId", is(1))); // Verify response content
    }
}

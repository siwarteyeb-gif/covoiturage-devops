package com.example.notificationservice.controller;

import com.example.notificationservice.dto.Notification;
import com.example.notificationservice.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificationControllerTest {

    private NotificationService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(NotificationService.class);
        mvc = MockMvcBuilders.standaloneSetup(new NotificationController(service)).build();
    }

    @Test
    void validNotificationIsAccepted() throws Exception {
        when(service.send(any(Notification.class))).thenReturn(true);

        mvc.perform(post("/notifications").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"to\":\"karim@test.tn\",\"subject\":\"Sujet\",\"body\":\"Corps\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("accepted"))
                .andExpect(jsonPath("$.sent").value(true));

        verify(service).send(new Notification("karim@test.tn", "Sujet", "Corps"));
    }

    @Test
    void invalidRecipientIsRejected() throws Exception {
        mvc.perform(post("/notifications").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"to\":\"pas-un-email\",\"subject\":\"Sujet\",\"body\":\"Corps\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void healthReturnsUp() throws Exception {
        mvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("notification-service"));
    }
}

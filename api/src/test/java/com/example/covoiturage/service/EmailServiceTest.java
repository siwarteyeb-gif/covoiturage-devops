package com.example.covoiturage.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class EmailServiceTest {

    private MockRestServiceServer server;
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://notification-service");
        server = MockRestServiceServer.bindTo(builder).build();
        emailService = new EmailService(builder.build());
    }

    @Test
    void sendPostsTheNotificationToTheNotificationService() {
        server.expect(requestTo("http://notification-service/notifications"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.to").value("karim@test.tn"))
                .andExpect(jsonPath("$.subject").value("Réservation acceptée"))
                .andExpect(jsonPath("$.body").value("Votre place est confirmée."))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        emailService.send("karim@test.tn", "Réservation acceptée", "Votre place est confirmée.");

        server.verify();
    }

    @Test
    void sendNeverThrowsWhenTheServiceIsDown() {
        server.expect(requestTo("http://notification-service/notifications"))
                .andRespond(withServerError());

        assertDoesNotThrow(() -> emailService.send("a@b.tn", "Sujet", "Corps"));
    }
}

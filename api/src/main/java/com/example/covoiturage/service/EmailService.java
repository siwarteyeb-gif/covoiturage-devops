package com.example.covoiturage.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Client HTTP du micro-service "notification-service" (F23).
 * L'API ne parle plus au serveur SMTP : elle délègue l'envoi au notification-service.
 * Une erreur d'envoi ne casse jamais l'opération en cours (réservation, annulation...).
 */
@Service
@Slf4j
public class EmailService {

    private final RestClient client;

    @Autowired
    public EmailService(@Value("${notification-service.url:http://localhost:8082}") String baseUrl) {
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    /** Constructeur utilisé par les tests (RestClient branché sur un faux serveur). */
    EmailService(RestClient client) {
        this.client = client;
    }

    /** Corps JSON envoyé au notification-service. */
    public record Notification(String to, String subject, String body) {
    }

    public void send(String to, String subject, String body) {
        try {
            client.post()
                    .uri("/notifications")
                    .body(new Notification(to, subject, body))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException e) {
            log.warn("Notification non envoyée à {} (notification-service indisponible) : {}", to, e.getMessage());
        }
    }
}

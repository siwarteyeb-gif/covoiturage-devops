package com.example.notificationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Micro-service "notification-service" : envoi des emails (réservations, annulations...).
 * Seule l'API l'appelle, par le réseau interne Docker (port non publié).
 */
@SpringBootApplication
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}

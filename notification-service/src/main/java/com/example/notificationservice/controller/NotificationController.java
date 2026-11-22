package com.example.notificationservice.controller;

import com.example.notificationservice.dto.Notification;
import com.example.notificationservice.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @PostMapping("/notifications")
    public ResponseEntity<Map<String, Object>> send(@RequestBody Notification notification) {
        if (!notification.isValid()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Destinataire ou sujet invalide"));
        }
        boolean sent = service.send(notification);
        return ResponseEntity.accepted().body(Map.of("status", "accepted", "sent", sent));
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "notification-service");
    }
}

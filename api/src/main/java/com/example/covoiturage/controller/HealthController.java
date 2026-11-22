package com.example.covoiturage.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Route de santé utilisée par Docker (HEALTHCHECK), Compose et le pipeline (contrôle après mise en service). */
@RestController
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "api");
    }
}

package com.example.mapservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Micro-service "map-service" : géocodage et calcul d'itinéraire.
 * Il est le seul à connaître la clé OpenRouteService. Il n'est pas exposé à l'extérieur :
 * seule l'API l'appelle, par le réseau interne Docker.
 */
@SpringBootApplication
public class MapServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MapServiceApplication.class, args);
    }
}

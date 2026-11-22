package com.example.mapservice.controller;

import com.example.mapservice.service.RoutingService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contrôleurs testés sans démarrer Spring : service réel en mode secours (pas de clé API). */
class MapControllerTest {

    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new MapController(new RoutingService("", "http://localhost:1")), new HealthController())
            .build();

    @Test
    void geocodeReturnsMatchingPlaces() throws Exception {
        mvc.perform(get("/geocode").param("query", "sousse"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].label").value(org.hamcrest.Matchers.hasItem("Sousse, Tunisie")))
                .andExpect(jsonPath("$[0].lat").isNumber());
    }

    @Test
    void geocodeRequiresTheQueryParameter() throws Exception {
        mvc.perform(get("/geocode")).andExpect(status().isBadRequest());
    }

    @Test
    void routeReturnsDistanceDurationAndPoints() throws Exception {
        mvc.perform(get("/route")
                        .param("fromLat", "36.8065").param("fromLng", "10.1815")
                        .param("toLat", "35.8256").param("toLng", "10.6369"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("estimation"))
                .andExpect(jsonPath("$.distanceKm").isNumber())
                .andExpect(jsonPath("$.points.length()").value(2))
                .andExpect(jsonPath("$.points[0][0]").value(36.8065));
    }

    @Test
    void healthReturnsUp() throws Exception {
        mvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("map-service"));
    }
}

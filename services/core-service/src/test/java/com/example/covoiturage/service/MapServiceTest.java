package com.example.covoiturage.service;

import com.example.covoiturage.dto.PlaceResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Sans clé API, MapService utilise le calcul de secours : aucun appel réseau n'est fait. */
class MapServiceTest {

    private final MapService mapService = new MapService("", "http://localhost:1");

    @Test
    void autocompleteUsesLocalGazetteerWithoutApiKey() {
        List<PlaceResult> places = mapService.autocomplete("manou");
        assertFalse(places.isEmpty());
        assertTrue(places.stream().anyMatch(p -> p.label().contains("Manouba")));
    }

    @Test
    void autocompleteIgnoresTooShortText() {
        assertTrue(mapService.autocomplete("a").isEmpty());
    }

    @Test
    void routeFallsBackToEstimation() {
        MapService.RouteResult r = mapService.route(36.8665, 10.1647, 36.8101, 10.0863);
        assertEquals("estimation", r.source());
        assertTrue(r.distanceKm() > 5 && r.distanceKm() < 20, "distance = " + r.distanceKm());
        assertTrue(r.durationMin() >= 1);
        assertEquals(2, r.points().size());
    }
}

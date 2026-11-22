package com.example.mapservice.service;

import com.example.mapservice.dto.PlaceResult;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RoutingServiceTest {

    /** Sans clé API : calcul de secours, aucun appel réseau. */
    @Test
    void withoutApiKeyAutocompleteUsesTheLocalGazetteer() {
        RoutingService service = new RoutingService("", "http://localhost:1");

        List<PlaceResult> places = service.autocomplete("manou");

        assertFalse(places.isEmpty());
        assertTrue(places.stream().anyMatch(p -> p.label().contains("Manouba")));
    }

    @Test
    void autocompleteIgnoresTooShortText() {
        assertTrue(new RoutingService("", "http://localhost:1").autocomplete("a").isEmpty());
    }

    @Test
    void withoutApiKeyRouteFallsBackToEstimation() {
        RoutingService.RouteResult r = new RoutingService("", "http://localhost:1")
                .route(36.8665, 10.1647, 36.8101, 10.0863);

        assertEquals("estimation", r.source());
        assertTrue(r.distanceKm() > 5 && r.distanceKm() < 20, "distance = " + r.distanceKm());
        assertTrue(r.durationMin() >= 1);
        assertEquals(2, r.points().size());
    }

    // ----- avec une clé API : OpenRouteService est simulé -----

    private MockRestServiceServer server;

    private RoutingService withKey() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        return new RoutingService("ma-cle", "http://ors", builder.build());
    }

    @Test
    void routeParsesTheOpenRouteServiceResponse() {
        RoutingService service = withKey();
        server.expect(requestTo(Matchers.startsWith("http://ors/v2/directions/driving-car")))
                .andRespond(withSuccess(
                        "{\"features\":[{\"properties\":{\"summary\":{\"distance\":142500.0,\"duration\":5700.0}},"
                                + "\"geometry\":{\"coordinates\":[[10.18,36.80],[10.40,36.10],[10.63,35.82]]}}]}",
                        MediaType.APPLICATION_JSON));

        RoutingService.RouteResult r = service.route(36.80, 10.18, 35.82, 10.63);

        assertEquals("openrouteservice", r.source());
        assertEquals(142.5, r.distanceKm(), 0.001);
        assertEquals(95, r.durationMin());
        assertTrue(r.points().size() >= 3);
        // ORS renvoie [lng, lat] : le service doit renvoyer [lat, lng]
        assertEquals(36.80, r.points().get(0)[0], 0.0001);
        assertEquals(10.18, r.points().get(0)[1], 0.0001);
        server.verify();
    }

    @Test
    void routeFallsBackWhenOpenRouteServiceFails() {
        RoutingService service = withKey();
        server.expect(requestTo(Matchers.startsWith("http://ors/v2/directions")))
                .andRespond(withServerError());

        assertEquals("estimation", service.route(36.80, 10.18, 35.82, 10.63).source());
    }

    @Test
    void autocompleteParsesTheOpenRouteServiceResponse() {
        RoutingService service = withKey();
        server.expect(requestTo(Matchers.startsWith("http://ors/geocode/autocomplete")))
                .andRespond(withSuccess(
                        "{\"features\":[{\"properties\":{\"label\":\"Sfax, Tunisie\"},"
                                + "\"geometry\":{\"coordinates\":[10.76,34.74]}}]}",
                        MediaType.APPLICATION_JSON));

        List<PlaceResult> places = service.autocomplete("sfax");

        assertEquals(1, places.size());
        assertEquals("Sfax, Tunisie", places.get(0).label());
        assertEquals(34.74, places.get(0).lat(), 0.0001);
        assertEquals(10.76, places.get(0).lng(), 0.0001);
    }

    @Test
    void autocompleteFallsBackToTheGazetteerWhenOpenRouteServiceFails() {
        RoutingService service = withKey();
        server.expect(requestTo(Matchers.startsWith("http://ors/geocode/autocomplete")))
                .andRespond(withServerError());

        assertTrue(service.autocomplete("sousse").stream().anyMatch(p -> p.label().contains("Sousse")));
    }
}

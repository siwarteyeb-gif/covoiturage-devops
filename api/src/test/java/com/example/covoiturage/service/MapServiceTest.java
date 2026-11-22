package com.example.covoiturage.service;

import com.example.covoiturage.dto.PlaceResult;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Le map-service est simulé par un faux serveur : aucun appel réseau réel n'est fait. */
class MapServiceTest {

    private MockRestServiceServer server;
    private MapService mapService;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://map-service");
        server = MockRestServiceServer.bindTo(builder).build();
        mapService = new MapService(builder.build());
    }

    @Test
    void autocompleteReturnsPlacesFromMapService() {
        server.expect(requestTo("http://map-service/geocode?query=sousse"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "[{\"label\":\"Sousse, Tunisie\",\"lat\":35.8256,\"lng\":10.6369}]",
                        MediaType.APPLICATION_JSON));

        List<PlaceResult> places = mapService.autocomplete("sousse");

        assertEquals(1, places.size());
        assertEquals("Sousse, Tunisie", places.get(0).label());
        assertEquals(35.8256, places.get(0).lat(), 0.0001);
        server.verify();
    }

    @Test
    void autocompleteIgnoresTooShortTextWithoutCallingTheService() {
        assertTrue(mapService.autocomplete("a").isEmpty());
        assertTrue(mapService.autocomplete(null).isEmpty());
        server.verify(); // aucune requête attendue, aucune reçue
    }

    @Test
    void autocompleteReturnsEmptyListWhenServiceFails() {
        server.expect(requestTo(Matchers.startsWith("http://map-service/geocode")))
                .andRespond(withServerError());

        assertTrue(mapService.autocomplete("tunis").isEmpty());
    }

    @Test
    void routeMapsTheRemoteResponse() {
        server.expect(requestTo(Matchers.startsWith("http://map-service/route")))
                .andRespond(withSuccess(
                        "{\"distanceKm\":142.5,\"durationMin\":95,"
                                + "\"points\":[[36.8,10.18],[36.1,10.4],[35.82,10.63]],"
                                + "\"source\":\"openrouteservice\"}",
                        MediaType.APPLICATION_JSON));

        MapService.RouteResult r = mapService.route(36.8, 10.18, 35.82, 10.63);

        assertEquals("openrouteservice", r.source());
        assertEquals(142.5, r.distanceKm(), 0.001);
        assertEquals(95, r.durationMin());
        assertEquals(3, r.points().size());
        assertEquals(36.1, r.points().get(1)[0], 0.0001);
    }

    @Test
    void routeFallsBackToEstimationWhenServiceFails() {
        server.expect(requestTo(Matchers.startsWith("http://map-service/route")))
                .andRespond(withServerError());

        MapService.RouteResult r = mapService.route(36.8665, 10.1647, 36.8101, 10.0863);

        assertEquals("estimation", r.source());
        assertTrue(r.distanceKm() > 5 && r.distanceKm() < 20, "distance = " + r.distanceKm());
        assertEquals(2, r.points().size());
    }

    @Test
    void routeFallsBackToEstimationWhenResponseHasTooFewPoints() {
        server.expect(requestTo(Matchers.startsWith("http://map-service/route")))
                .andRespond(withSuccess(
                        "{\"distanceKm\":1.0,\"durationMin\":1,\"points\":[[36.8,10.18]],\"source\":\"x\"}",
                        MediaType.APPLICATION_JSON));

        assertEquals("estimation", mapService.route(36.8, 10.18, 35.82, 10.63).source());
    }

    @Test
    void estimateRouteUsesRoadFactorAndStraightLine() {
        MapService.RouteResult r = mapService.estimateRoute(36.8065, 10.1815, 35.8256, 10.6369);

        assertEquals("estimation", r.source());
        assertTrue(r.distanceKm() > 140 && r.distanceKm() < 190, "distance = " + r.distanceKm());
        assertTrue(r.durationMin() > 100);
        assertEquals(2, r.points().size());
    }
}

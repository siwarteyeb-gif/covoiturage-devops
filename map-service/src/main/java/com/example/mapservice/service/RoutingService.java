package com.example.mapservice.service;

import com.example.mapservice.dto.PlaceResult;
import com.example.mapservice.util.GeoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Accès à l'API externe OpenRouteService (géocodage et itinéraire).
 * Sans clé API, ou si le service est indisponible, on utilise un calcul de secours :
 * annuaire local pour les adresses et formule de Haversine pour la distance.
 */
@Service
public class RoutingService {

    private static final Logger log = LoggerFactory.getLogger(RoutingService.class);

    private static final int MAX_POINTS = 250;
    private static final double ROAD_FACTOR = 1.3;     // la route est plus longue que la ligne droite
    private static final double AVERAGE_SPEED_KMH = 45.0;

    private final RestClient client;
    private final String apiKey;
    private final String baseUrl;

    @Autowired
    public RoutingService(@Value("${ors.api.key:}") String apiKey,
                          @Value("${ors.base-url:https://api.openrouteservice.org}") String baseUrl) {
        this(apiKey, baseUrl, RestClient.create());
    }

    /** Constructeur utilisé par les tests (RestClient branché sur un faux serveur). */
    RoutingService(String apiKey, String baseUrl, RestClient client) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.baseUrl = baseUrl;
        this.client = client;
    }

    /** Résultat d'un calcul d'itinéraire. Les points du tracé sont des tableaux {lat, lng}. */
    public record RouteResult(double distanceKm, int durationMin, List<double[]> points, String source) {
    }

    public boolean isConfigured() {
        return !apiKey.isEmpty();
    }

    /** F09 : autocomplétion d'adresses. */
    public List<PlaceResult> autocomplete(String text) {
        if (text == null || text.trim().length() < 2) {
            return List.of();
        }
        if (isConfigured()) {
            try {
                List<PlaceResult> places = orsAutocomplete(text.trim());
                if (!places.isEmpty()) {
                    return places;
                }
            } catch (RuntimeException e) {
                log.warn("OpenRouteService indisponible pour le géocodage : {}", e.getMessage());
            }
        }
        return Gazetteer.search(text);
    }

    /** F10 / F16 : itinéraire, distance et durée entre deux points. */
    public RouteResult route(double fromLat, double fromLng, double toLat, double toLng) {
        if (isConfigured()) {
            try {
                return orsRoute(fromLat, fromLng, toLat, toLng);
            } catch (RuntimeException e) {
                log.warn("OpenRouteService indisponible pour l'itinéraire : {}", e.getMessage());
            }
        }
        return estimateRoute(fromLat, fromLng, toLat, toLng);
    }

    /** Calcul de secours : distance à vol d'oiseau corrigée, tracé en ligne droite. */
    RouteResult estimateRoute(double fromLat, double fromLng, double toLat, double toLng) {
        double km = GeoUtils.haversineKm(fromLat, fromLng, toLat, toLng) * ROAD_FACTOR;
        double distance = Math.round(km * 10.0) / 10.0;
        int minutes = (int) Math.max(1, Math.round(km / AVERAGE_SPEED_KMH * 60.0));
        List<double[]> points = List.of(new double[]{fromLat, fromLng}, new double[]{toLat, toLng});
        return new RouteResult(distance, minutes, points, "estimation");
    }

    // ------------------------------------------------------------------ OpenRouteService

    private List<PlaceResult> orsAutocomplete(String text) {
        Map<String, Object> body = client.get()
                .uri(baseUrl + "/geocode/autocomplete?api_key={key}&text={text}&boundary.country=TN&size=6",
                        apiKey, text)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {
                });
        List<PlaceResult> places = new ArrayList<>();
        if (body == null || !(body.get("features") instanceof List<?> features)) {
            return places;
        }
        for (Object f : features) {
            Map<String, Object> feature = (Map<String, Object>) f;
            Map<String, Object> props = (Map<String, Object>) feature.get("properties");
            Map<String, Object> geometry = (Map<String, Object>) feature.get("geometry");
            List<Object> coords = (List<Object>) geometry.get("coordinates");
            String label = String.valueOf(props.get("label"));
            double lng = ((Number) coords.get(0)).doubleValue();
            double lat = ((Number) coords.get(1)).doubleValue();
            places.add(new PlaceResult(label, lat, lng));
        }
        return places;
    }

    private RouteResult orsRoute(double fromLat, double fromLng, double toLat, double toLng) {
        String start = fromLng + "," + fromLat;   // ORS attend longitude,latitude
        String end = toLng + "," + toLat;
        Map<String, Object> body = client.get()
                .uri(baseUrl + "/v2/directions/driving-car?api_key={key}&start={start}&end={end}",
                        apiKey, start, end)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {
                });
        List<Object> features = (List<Object>) body.get("features");
        Map<String, Object> feature = (Map<String, Object>) features.get(0);
        Map<String, Object> props = (Map<String, Object>) feature.get("properties");
        Map<String, Object> summary = (Map<String, Object>) props.get("summary");
        Map<String, Object> geometry = (Map<String, Object>) feature.get("geometry");
        List<Object> coordinates = (List<Object>) geometry.get("coordinates");

        double distanceKm = Math.round(((Number) summary.get("distance")).doubleValue() / 100.0) / 10.0;
        int durationMin = (int) Math.max(1, Math.round(((Number) summary.get("duration")).doubleValue() / 60.0));

        List<double[]> points = new ArrayList<>();
        int step = Math.max(1, (int) Math.ceil(coordinates.size() / (double) MAX_POINTS));
        for (int i = 0; i < coordinates.size(); i += step) {
            points.add(toLatLng(coordinates.get(i)));
        }
        points.add(toLatLng(coordinates.get(coordinates.size() - 1)));
        return new RouteResult(distanceKm, durationMin, points, "openrouteservice");
    }

    private static double[] toLatLng(Object lngLat) {
        List<Object> c = (List<Object>) lngLat;
        return new double[]{((Number) c.get(1)).doubleValue(), ((Number) c.get(0)).doubleValue()};
    }
}

package com.example.covoiturage.service;

import com.example.covoiturage.dto.PlaceResult;
import com.example.covoiturage.util.GeoUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Client HTTP du micro-service "map-service" (géocodage et itinéraire).
 * L'API n'appelle plus OpenRouteService elle-même : c'est le rôle du map-service.
 *
 * Résilience : si le map-service est injoignable ou répond une erreur, l'API continue de
 * fonctionner en mode dégradé (distance à vol d'oiseau pour l'itinéraire, liste vide pour
 * l'autocomplétion) au lieu de renvoyer une erreur 500 à l'utilisateur.
 */
@Service
@Slf4j
public class MapService {

    private static final double ROAD_FACTOR = 1.3;     // la route est plus longue que la ligne droite
    private static final double AVERAGE_SPEED_KMH = 45.0;

    private final RestClient client;

    @Autowired
    public MapService(@Value("${map-service.url:http://localhost:8081}") String baseUrl) {
        this(RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(timeouts())
                .build());
    }

    /** Constructeur utilisé par les tests (RestClient branché sur un faux serveur). */
    MapService(RestClient client) {
        this.client = client;
    }

    private static JdkClientHttpRequestFactory timeouts() {
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(8));
        return factory;
    }

    /** Résultat d'un calcul d'itinéraire. Les points du tracé sont des tableaux {lat, lng}. */
    public record RouteResult(double distanceKm, int durationMin, List<double[]> points, String source) {
    }

    /** Forme JSON renvoyée par le map-service pour /route. */
    public record RemoteRoute(double distanceKm, int durationMin, List<List<Double>> points, String source) {
    }

    /** F09 : autocomplétion d'adresses. */
    public List<PlaceResult> autocomplete(String text) {
        if (text == null || text.trim().length() < 2) {
            return List.of();
        }
        try {
            List<PlaceResult> places = client.get()
                    .uri("/geocode?query={q}", text.trim())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<PlaceResult>>() {
                    });
            return places == null ? List.of() : places;
        } catch (RuntimeException e) {
            log.warn("map-service indisponible pour le géocodage : {}", e.getMessage());
            return List.of();
        }
    }

    /** F10 / F16 : itinéraire, distance et durée entre deux points. */
    public RouteResult route(double fromLat, double fromLng, double toLat, double toLng) {
        try {
            RemoteRoute remote = client.get()
                    .uri("/route?fromLat={a}&fromLng={b}&toLat={c}&toLng={d}", fromLat, fromLng, toLat, toLng)
                    .retrieve()
                    .body(RemoteRoute.class);
            if (remote != null && remote.points() != null && remote.points().size() >= 2) {
                List<double[]> points = new ArrayList<>();
                for (List<Double> p : remote.points()) {
                    points.add(new double[]{p.get(0), p.get(1)});
                }
                return new RouteResult(remote.distanceKm(), remote.durationMin(), points, remote.source());
            }
            log.warn("Réponse invalide du map-service pour l'itinéraire, mode dégradé");
        } catch (RuntimeException e) {
            log.warn("map-service indisponible pour l'itinéraire : {}", e.getMessage());
        }
        return estimateRoute(fromLat, fromLng, toLat, toLng);
    }

    /** Mode dégradé : distance à vol d'oiseau corrigée, tracé en ligne droite. */
    RouteResult estimateRoute(double fromLat, double fromLng, double toLat, double toLng) {
        double km = GeoUtils.haversineKm(fromLat, fromLng, toLat, toLng) * ROAD_FACTOR;
        double distance = Math.round(km * 10.0) / 10.0;
        int minutes = (int) Math.max(1, Math.round(km / AVERAGE_SPEED_KMH * 60.0));
        List<double[]> points = List.of(new double[]{fromLat, fromLng}, new double[]{toLat, toLng});
        return new RouteResult(distance, minutes, points, "estimation");
    }
}

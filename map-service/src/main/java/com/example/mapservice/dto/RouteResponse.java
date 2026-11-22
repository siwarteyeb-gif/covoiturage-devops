package com.example.mapservice.dto;

import java.util.List;

/** Itinéraire : distance, durée, tracé (liste de [lat, lng]) et origine du calcul. */
public record RouteResponse(double distanceKm, int durationMin, List<List<Double>> points, String source) {
}

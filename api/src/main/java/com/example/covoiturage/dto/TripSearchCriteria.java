package com.example.covoiturage.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Critères de recherche d'un trajet (F12, F13, F14). */
public record TripSearchCriteria(
        String from, String to,
        Double fromLat, Double fromLng, Double toLat, Double toLng,
        LocalDate date, Integer seats,
        BigDecimal maxPrice, Double maxDetourKm,
        String sort
) {
}

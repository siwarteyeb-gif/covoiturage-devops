package com.example.covoiturage.dto;

import java.math.BigDecimal;
import java.util.List;

/** Itinéraire calculé + prix suggéré et prix maximum autorisé (RG8). */
public record RouteEstimateResponse(
        double distanceKm,
        int durationMin,
        List<List<Double>> route,
        BigDecimal suggestedPrice,
        BigDecimal maxPrice,
        String source
) {
}

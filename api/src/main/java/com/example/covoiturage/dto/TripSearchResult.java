package com.example.covoiturage.dto;

/** Un trajet trouvé par la recherche, avec le détour que le conducteur ferait (si calculé). */
public record TripSearchResult(TripResponse trip, Double detourKm) {
}

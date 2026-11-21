package com.example.covoiturage.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record TripRequest(
        @NotNull(message = "Le véhicule est obligatoire")
        Long vehicleId,

        @NotBlank(message = "L'adresse de départ est obligatoire")
        @Size(max = 255, message = "Adresse trop longue")
        String departureAddress,
        @NotNull(message = "La latitude de départ est obligatoire")
        @DecimalMin(value = "-90.0", message = "Latitude invalide") @DecimalMax(value = "90.0", message = "Latitude invalide")
        Double departureLat,
        @NotNull(message = "La longitude de départ est obligatoire")
        @DecimalMin(value = "-180.0", message = "Longitude invalide") @DecimalMax(value = "180.0", message = "Longitude invalide")
        Double departureLng,

        @NotBlank(message = "L'adresse d'arrivée est obligatoire")
        @Size(max = 255, message = "Adresse trop longue")
        String arrivalAddress,
        @NotNull(message = "La latitude d'arrivée est obligatoire")
        @DecimalMin(value = "-90.0", message = "Latitude invalide") @DecimalMax(value = "90.0", message = "Latitude invalide")
        Double arrivalLat,
        @NotNull(message = "La longitude d'arrivée est obligatoire")
        @DecimalMin(value = "-180.0", message = "Longitude invalide") @DecimalMax(value = "180.0", message = "Longitude invalide")
        Double arrivalLng,

        @NotNull(message = "La date et l'heure de départ sont obligatoires")
        LocalDateTime departureDateTime,

        @NotNull(message = "Le nombre de places est obligatoire")
        @Min(value = 1, message = "Minimum 1 place") @Max(value = 8, message = "Maximum 8 places")
        Integer totalSeats,

        @NotNull(message = "Le prix est obligatoire")
        @DecimalMin(value = "0.0", message = "Le prix ne peut pas être négatif")
        BigDecimal pricePerSeat,

        /** Trajet récurrent : jours de la semaine (1 = lundi ... 7 = dimanche). */
        List<@Min(1) @Max(7) Integer> recurrenceWeekdays,

        /** Trajet récurrent : nombre de semaines à couvrir (0 à 12). */
        @Min(value = 0, message = "Minimum 0 semaine") @Max(value = 12, message = "Maximum 12 semaines")
        Integer recurrenceWeeks
) {
}

package com.example.covoiturage.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TripUpdateRequest(
        @NotNull(message = "La date et l'heure de départ sont obligatoires")
        LocalDateTime departureDateTime,

        @NotNull(message = "Le nombre de places est obligatoire")
        @Min(value = 1, message = "Minimum 1 place") @Max(value = 8, message = "Maximum 8 places")
        Integer totalSeats,

        @NotNull(message = "Le prix est obligatoire")
        @DecimalMin(value = "0.0", message = "Le prix ne peut pas être négatif")
        BigDecimal pricePerSeat
) {
}

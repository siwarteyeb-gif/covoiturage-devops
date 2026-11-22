package com.example.covoiturage.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record BookingRequest(
        @NotNull(message = "Le nombre de places est obligatoire")
        @Min(value = 1, message = "Minimum 1 place") @Max(value = 8, message = "Maximum 8 places")
        Integer seats
) {
}

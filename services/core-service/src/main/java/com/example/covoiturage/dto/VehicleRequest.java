package com.example.covoiturage.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record VehicleRequest(
        @NotBlank(message = "La marque est obligatoire")
        @Size(max = 50, message = "La marque ne doit pas dépasser 50 caractères")
        String brand,

        @NotBlank(message = "Le modèle est obligatoire")
        @Size(max = 50, message = "Le modèle ne doit pas dépasser 50 caractères")
        String model,

        @Size(max = 30, message = "La couleur ne doit pas dépasser 30 caractères")
        String color,

        @NotNull(message = "Le nombre de places est obligatoire")
        @Min(value = 1, message = "Minimum 1 place")
        @Max(value = 8, message = "Maximum 8 places")
        Integer seats
) {
}

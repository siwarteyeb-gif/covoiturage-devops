package com.example.covoiturage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "Le nom est obligatoire")
        @Size(max = 100, message = "Le nom ne doit pas dépasser 100 caractères")
        String name,

        @NotBlank(message = "Le téléphone est obligatoire")
        @Pattern(regexp = "^(\\+216)?[0-9]{8}$", message = "Téléphone invalide (8 chiffres, ex. 22123456)")
        String phone
) {
}

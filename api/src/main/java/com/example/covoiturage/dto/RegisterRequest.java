package com.example.covoiturage.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Le nom est obligatoire")
        @Size(max = 100, message = "Le nom ne doit pas dépasser 100 caractères")
        String name,

        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "Email invalide")
        @Size(max = 150, message = "L'email ne doit pas dépasser 150 caractères")
        String email,

        @NotBlank(message = "Le téléphone est obligatoire")
        @Pattern(regexp = "^(\\+216)?[0-9]{8}$", message = "Téléphone invalide (8 chiffres, ex. 22123456)")
        String phone,

        @NotBlank(message = "Le mot de passe est obligatoire")
        @Size(min = 8, max = 100, message = "Le mot de passe doit contenir entre 8 et 100 caractères")
        String password
) {
}

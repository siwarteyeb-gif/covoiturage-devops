package com.example.covoiturage.dto;

import com.example.covoiturage.entity.TripStatus;
import jakarta.validation.constraints.NotNull;

public record TripStatusRequest(@NotNull(message = "Le statut est obligatoire") TripStatus status) {
}

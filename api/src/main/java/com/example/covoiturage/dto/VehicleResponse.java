package com.example.covoiturage.dto;

import com.example.covoiturage.entity.Vehicle;

public record VehicleResponse(
        Long id,
        String brand,
        String model,
        String color,
        int seats
) {
    public static VehicleResponse from(Vehicle v) {
        return new VehicleResponse(v.getId(), v.getBrand(), v.getModel(), v.getColor(), v.getSeats());
    }
}

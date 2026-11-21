package com.example.covoiturage.dto;

import java.util.List;

public record StatsResponse(
        long usersCount,
        long driversCount,
        long tripsCount,
        long openTripsCount,
        long bookingsCount,
        long acceptedBookings,
        double fillRatePercent,
        List<PopularRoute> popularRoutes
) {
    public record PopularRoute(String departure, String arrival, long bookings) {
    }
}

package com.example.covoiturage.dto;

import com.example.covoiturage.entity.Trip;
import com.example.covoiturage.entity.TripStatus;
import com.example.covoiturage.util.RouteCodec;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record TripResponse(
        Long id,
        PersonSummary driver,
        VehicleResponse vehicle,
        String departureAddress,
        double departureLat,
        double departureLng,
        String arrivalAddress,
        double arrivalLat,
        double arrivalLng,
        LocalDateTime departureDateTime,
        int totalSeats,
        int availableSeats,
        BigDecimal pricePerSeat,
        double distanceKm,
        int durationMin,
        TripStatus status,
        List<List<Double>> route,
        String seriesId
) {
    /** withRoute = true uniquement pour le détail d'un trajet (le tracé peut être long). */
    public static TripResponse from(Trip t, boolean withRoute) {
        return new TripResponse(
                t.getId(),
                PersonSummary.from(t.getDriver(), false),
                VehicleResponse.from(t.getVehicle()),
                t.getDepartureAddress(), t.getDepartureLat(), t.getDepartureLng(),
                t.getArrivalAddress(), t.getArrivalLat(), t.getArrivalLng(),
                t.getDepartureDateTime(), t.getTotalSeats(), t.getAvailableSeats(),
                t.getPricePerSeat(), t.getDistanceKm(), t.getDurationMin(), t.getStatus(),
                withRoute ? RouteCodec.decode(t.getRouteGeometry()) : null,
                t.getSeriesId());
    }
}

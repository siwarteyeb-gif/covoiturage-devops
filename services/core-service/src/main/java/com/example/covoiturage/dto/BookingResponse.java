package com.example.covoiturage.dto;

import com.example.covoiturage.entity.Booking;
import com.example.covoiturage.entity.BookingStatus;
import com.example.covoiturage.entity.Trip;
import com.example.covoiturage.entity.TripStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BookingResponse(
        Long id,
        TripSummary trip,
        PersonSummary passenger,
        int seats,
        BookingStatus status,
        LocalDateTime createdAt,
        boolean reviewedByMe
) {
    public record TripSummary(Long id, String departureAddress, String arrivalAddress,
                              LocalDateTime departureDateTime, BigDecimal pricePerSeat,
                              TripStatus status, PersonSummary driver) {
    }

    /** Les numéros de téléphone ne sont visibles que lorsque la réservation est acceptée. */
    public static BookingResponse from(Booking b, boolean reviewedByMe) {
        boolean accepted = b.getStatus() == BookingStatus.ACCEPTED;
        Trip t = b.getTrip();
        TripSummary summary = new TripSummary(t.getId(), t.getDepartureAddress(), t.getArrivalAddress(),
                t.getDepartureDateTime(), t.getPricePerSeat(), t.getStatus(),
                PersonSummary.from(t.getDriver(), accepted));
        return new BookingResponse(b.getId(), summary, PersonSummary.from(b.getPassenger(), accepted),
                b.getSeats(), b.getStatus(), b.getCreatedAt(), reviewedByMe);
    }
}

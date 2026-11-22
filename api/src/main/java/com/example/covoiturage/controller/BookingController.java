package com.example.covoiturage.controller;

import com.example.covoiturage.dto.BookingRequest;
import com.example.covoiturage.dto.BookingResponse;
import com.example.covoiturage.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping("/trips/{tripId}/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse request(Authentication auth, @PathVariable Long tripId,
                                   @Valid @RequestBody BookingRequest request) {
        return bookingService.request(auth.getName(), tripId, request);
    }

    @GetMapping("/trips/{tripId}/bookings")
    public List<BookingResponse> tripBookings(Authentication auth, @PathVariable Long tripId) {
        return bookingService.tripBookings(auth.getName(), tripId);
    }

    @GetMapping("/bookings/me")
    public List<BookingResponse> mine(Authentication auth) {
        return bookingService.myBookings(auth.getName());
    }

    @GetMapping("/bookings/received")
    public List<BookingResponse> received(Authentication auth) {
        return bookingService.receivedRequests(auth.getName());
    }

    @PatchMapping("/bookings/{id}/accept")
    public BookingResponse accept(Authentication auth, @PathVariable Long id) {
        return bookingService.accept(auth.getName(), id);
    }

    @PatchMapping("/bookings/{id}/reject")
    public BookingResponse reject(Authentication auth, @PathVariable Long id) {
        return bookingService.reject(auth.getName(), id);
    }

    @PatchMapping("/bookings/{id}/cancel")
    public BookingResponse cancel(Authentication auth, @PathVariable Long id) {
        return bookingService.cancel(auth.getName(), id);
    }
}

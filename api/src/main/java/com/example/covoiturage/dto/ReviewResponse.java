package com.example.covoiturage.dto;

import com.example.covoiturage.entity.Review;

import java.time.LocalDateTime;

public record ReviewResponse(Long id, Long bookingId, PersonSummary author, PersonSummary target,
                             int rating, String comment, LocalDateTime createdAt) {

    public static ReviewResponse from(Review r) {
        return new ReviewResponse(r.getId(), r.getBooking().getId(),
                PersonSummary.from(r.getAuthor(), false), PersonSummary.from(r.getTarget(), false),
                r.getRating(), r.getComment(), r.getCreatedAt());
    }
}

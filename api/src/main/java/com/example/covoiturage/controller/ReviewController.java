package com.example.covoiturage.controller;

import com.example.covoiturage.dto.ReviewRequest;
import com.example.covoiturage.dto.ReviewResponse;
import com.example.covoiturage.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/bookings/{bookingId}/review")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse create(Authentication auth, @PathVariable Long bookingId,
                                 @Valid @RequestBody ReviewRequest request) {
        return reviewService.create(auth.getName(), bookingId, request);
    }

    @GetMapping("/users/{userId}/reviews")
    public List<ReviewResponse> reviewsOf(@PathVariable Long userId) {
        return reviewService.reviewsOf(userId);
    }
}

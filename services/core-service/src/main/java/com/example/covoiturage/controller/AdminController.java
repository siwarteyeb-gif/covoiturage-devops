package com.example.covoiturage.controller;

import com.example.covoiturage.dto.AdminUserResponse;
import com.example.covoiturage.dto.ReviewResponse;
import com.example.covoiturage.dto.StatsResponse;
import com.example.covoiturage.dto.TripResponse;
import com.example.covoiturage.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    public List<AdminUserResponse> users() {
        return adminService.users();
    }

    @PatchMapping("/users/{id}/block")
    public AdminUserResponse block(Authentication auth, @PathVariable Long id) {
        return adminService.setBlocked(id, true, auth.getName());
    }

    @PatchMapping("/users/{id}/unblock")
    public AdminUserResponse unblock(Authentication auth, @PathVariable Long id) {
        return adminService.setBlocked(id, false, auth.getName());
    }

    @GetMapping("/trips")
    public List<TripResponse> trips() {
        return adminService.trips();
    }

    @DeleteMapping("/trips/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTrip(@PathVariable Long id) {
        adminService.deleteTrip(id);
    }

    @GetMapping("/reviews")
    public List<ReviewResponse> reviews() {
        return adminService.reviews();
    }

    @DeleteMapping("/reviews/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReview(@PathVariable Long id) {
        adminService.deleteReview(id);
    }

    @GetMapping("/stats")
    public StatsResponse stats() {
        return adminService.stats();
    }
}

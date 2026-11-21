package com.example.covoiturage.dto;

import com.example.covoiturage.entity.Role;
import com.example.covoiturage.entity.User;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String name,
        String email,
        String phone,
        Role role,
        double averageRating,
        LocalDateTime createdAt
) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getPhone(),
                u.getRole(), u.getAverageRating(), u.getCreatedAt());
    }
}

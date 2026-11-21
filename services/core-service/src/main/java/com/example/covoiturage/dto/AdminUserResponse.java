package com.example.covoiturage.dto;

import com.example.covoiturage.entity.Role;
import com.example.covoiturage.entity.User;

import java.time.LocalDateTime;

public record AdminUserResponse(Long id, String name, String email, String phone, Role role,
                                boolean blocked, double averageRating, LocalDateTime createdAt) {

    public static AdminUserResponse from(User u) {
        return new AdminUserResponse(u.getId(), u.getName(), u.getEmail(), u.getPhone(), u.getRole(),
                u.isBlocked(), u.getAverageRating(), u.getCreatedAt());
    }
}

package com.example.covoiturage.dto;

import com.example.covoiturage.entity.User;

/** Résumé public d'un utilisateur (le téléphone n'est partagé qu'après acceptation d'une réservation). */
public record PersonSummary(Long id, String name, double averageRating, String phone) {

    public static PersonSummary from(User u, boolean withPhone) {
        return new PersonSummary(u.getId(), u.getName(), u.getAverageRating(), withPhone ? u.getPhone() : null);
    }
}

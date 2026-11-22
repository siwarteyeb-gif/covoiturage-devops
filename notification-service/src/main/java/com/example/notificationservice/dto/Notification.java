package com.example.notificationservice.dto;

/** Message à envoyer. */
public record Notification(String to, String subject, String body) {

    public boolean isValid() {
        return to != null && !to.isBlank() && to.contains("@")
                && subject != null && !subject.isBlank()
                && body != null;
    }
}

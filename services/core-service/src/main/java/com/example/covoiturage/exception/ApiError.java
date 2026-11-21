package com.example.covoiturage.exception;

import java.time.LocalDateTime;
import java.util.Map;

/** Format unique des erreurs renvoyées par l'API. */
public record ApiError(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        Map<String, String> fieldErrors
) {
    public static ApiError of(int status, String error, String message) {
        return new ApiError(LocalDateTime.now(), status, error, message, Map.of());
    }
}

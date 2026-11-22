package com.example.covoiturage.security;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDateTime;

/** Réponses JSON d'erreur écrites directement par la couche sécurité (401 / 403). */
public final class SecurityJsonResponses {

    private SecurityJsonResponses() {
    }

    // Les messages sont des constantes sans guillemets : aucun risque d'injection dans le JSON.
    public static void write(HttpServletResponse response, int status, String error, String message)
            throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        String body = "{\"timestamp\":\"" + LocalDateTime.now() + "\","
                + "\"status\":" + status + ","
                + "\"error\":\"" + error + "\","
                + "\"message\":\"" + message + "\","
                + "\"fieldErrors\":{}}";
        response.getWriter().write(body);
    }
}

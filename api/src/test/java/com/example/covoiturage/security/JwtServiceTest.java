package com.example.covoiturage.security;

import com.example.covoiturage.entity.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-123456";

    @Test
    void generatedTokenContainsEmailAndIsValid() {
        JwtService jwtService = new JwtService(SECRET, 60_000);

        String token = jwtService.generateToken("karim@test.tn", Role.USER);

        assertEquals("karim@test.tn", jwtService.extractUsername(token));
        assertTrue(jwtService.isTokenValid(token, "karim@test.tn"));
    }

    @Test
    void tokenIsInvalidForAnotherUser() {
        JwtService jwtService = new JwtService(SECRET, 60_000);

        String token = jwtService.generateToken("karim@test.tn", Role.USER);

        assertFalse(jwtService.isTokenValid(token, "salma@test.tn"));
    }

    @Test
    void expiredTokenIsInvalid() {
        JwtService jwtService = new JwtService(SECRET, -1_000);

        String token = jwtService.generateToken("karim@test.tn", Role.USER);

        assertFalse(jwtService.isTokenValid(token, "karim@test.tn"));
    }

    @Test
    void tamperedTokenIsInvalid() {
        JwtService jwtService = new JwtService(SECRET, 60_000);

        String token = jwtService.generateToken("karim@test.tn", Role.USER);

        assertFalse(jwtService.isTokenValid(token + "x", "karim@test.tn"));
    }

    @Test
    void tokenSignedWithAnotherSecretIsInvalid() {
        JwtService other = new JwtService("another-secret-another-secret-123456789", 60_000);
        JwtService jwtService = new JwtService(SECRET, 60_000);

        String foreignToken = other.generateToken("karim@test.tn", Role.USER);

        assertFalse(jwtService.isTokenValid(foreignToken, "karim@test.tn"));
    }

    @Test
    void shortSecretIsRejected() {
        assertThrows(IllegalStateException.class, () -> new JwtService("too-short", 60_000));
    }
}

package com.example.mapservice.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeoUtilsTest {

    @Test
    void haversineTunisSousseIsAboutOneHundredSeventeenKm() {
        double km = GeoUtils.haversineKm(36.8065, 10.1815, 35.8256, 10.6369);
        assertTrue(km > 110 && km < 125, "distance = " + km);
    }

    @Test
    void haversineOfTheSamePointIsZero() {
        assertEquals(0.0, GeoUtils.haversineKm(36.8, 10.1, 36.8, 10.1), 0.0001);
    }

    @Test
    void normalizeRemovesAccentsAndLowercases() {
        assertEquals("gabes", GeoUtils.normalize("  Gabès "));
        assertEquals("", GeoUtils.normalize(null));
    }
}

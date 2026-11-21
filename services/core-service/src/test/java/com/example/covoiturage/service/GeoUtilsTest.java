package com.example.covoiturage.service;

import com.example.covoiturage.util.GeoUtils;
import com.example.covoiturage.util.RouteCodec;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GeoUtilsTest {

    @Test
    void haversineBetweenTunisAndSousseIsAbout120Km() {
        double km = GeoUtils.haversineKm(36.8065, 10.1815, 35.8256, 10.6369);
        assertTrue(km > 115 && km < 130, "distance = " + km);
    }

    @Test
    void passengerOnTheDriversRouteMeansNoDetour() {
        double detour = GeoUtils.detourKm(36.0, 10.0, 36.0, 10.2, 36.0, 10.05, 36.0, 10.15);
        assertTrue(detour < 0.1, "detour = " + detour);
    }

    @Test
    void passengerFarFromTheRouteMeansBigDetour() {
        double detour = GeoUtils.detourKm(36.0, 10.0, 36.0, 10.2, 36.0, 10.05, 37.0, 10.15);
        assertTrue(detour > 50, "detour = " + detour);
    }

    @Test
    void normalizeRemovesAccentsAndCase() {
        assertEquals("universite de la manouba", GeoUtils.normalize("  Université de la Manouba "));
    }

    @Test
    void routeCodecRoundTrip() {
        String text = RouteCodec.encode(List.of(new double[]{36.8065, 10.1815}, new double[]{35.8256, 10.6369}));
        List<List<Double>> points = RouteCodec.decode(text);
        assertEquals(2, points.size());
        assertEquals(36.8065, points.get(0).get(0), 0.00001);
        assertEquals(10.6369, points.get(1).get(1), 0.00001);
    }
}

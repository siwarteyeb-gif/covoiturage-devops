package com.example.covoiturage.util;

import java.text.Normalizer;
import java.util.Locale;

public final class GeoUtils {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private GeoUtils() {
    }

    /** Distance à vol d'oiseau entre deux points GPS (formule de Haversine), en km. */
    public static double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /**
     * Détour (km) que fait le conducteur pour prendre un passager :
     * A -> P -> D -> B moins le trajet direct A -> B.
     * A, B : départ et arrivée du conducteur ; P, D : départ et arrivée du passager.
     */
    public static double detourKm(double aLat, double aLng, double bLat, double bLng,
                                  double pLat, double pLng, double dLat, double dLng) {
        double viaPassenger = haversineKm(aLat, aLng, pLat, pLng)
                + haversineKm(pLat, pLng, dLat, dLng)
                + haversineKm(dLat, dLng, bLat, bLng);
        double direct = haversineKm(aLat, aLng, bLat, bLng);
        return Math.max(0.0, viaPassenger - direct);
    }

    /** Minuscules et sans accents, pour comparer des textes. */
    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String decomposed = Normalizer.normalize(text, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).trim();
    }
}

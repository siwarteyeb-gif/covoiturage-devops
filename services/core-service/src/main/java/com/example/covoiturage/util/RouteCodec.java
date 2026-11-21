package com.example.covoiturage.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Convertit un tracé en texte "lat,lng;lat,lng;..." (stockage en base) et inversement. */
public final class RouteCodec {

    private RouteCodec() {
    }

    /** Chaque point est un tableau {lat, lng}. */
    public static String encode(List<double[]> points) {
        StringBuilder sb = new StringBuilder();
        for (double[] p : points) {
            if (sb.length() > 0) {
                sb.append(';');
            }
            sb.append(String.format(Locale.ROOT, "%.5f,%.5f", p[0], p[1]));
        }
        return sb.toString();
    }

    public static List<List<Double>> decode(String text) {
        List<List<Double>> result = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return result;
        }
        for (String pair : text.split(";")) {
            String[] parts = pair.split(",");
            if (parts.length == 2) {
                result.add(List.of(Double.parseDouble(parts[0]), Double.parseDouble(parts[1])));
            }
        }
        return result;
    }
}

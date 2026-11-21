package com.example.covoiturage.service;

import com.example.covoiturage.dto.PlaceResult;
import com.example.covoiturage.util.GeoUtils;

import java.util.List;

/**
 * Petit annuaire de lieux tunisiens (coordonnées approximatives), utilisé quand la clé
 * OpenRouteService n'est pas configurée ou que le service est indisponible (mode démo).
 */
final class Gazetteer {

    private static final List<PlaceResult> PLACES = List.of(
            new PlaceResult("Tunis, Tunisie", 36.8065, 10.1815),
            new PlaceResult("Ariana, Tunisie", 36.8665, 10.1647),
            new PlaceResult("La Manouba, Tunisie", 36.8101, 10.0863),
            new PlaceResult("Université de la Manouba, Tunisie", 36.8190, 10.0620),
            new PlaceResult("Campus universitaire El Manar, Tunis", 36.8419, 10.1495),
            new PlaceResult("Ben Arous, Tunisie", 36.7531, 10.2189),
            new PlaceResult("Le Bardo, Tunisie", 36.8092, 10.1342),
            new PlaceResult("La Marsa, Tunisie", 36.8782, 10.3247),
            new PlaceResult("Carthage, Tunisie", 36.8528, 10.3233),
            new PlaceResult("Borj Cédria, Tunisie", 36.7000, 10.4300),
            new PlaceResult("Bizerte, Tunisie", 37.2744, 9.8739),
            new PlaceResult("Nabeul, Tunisie", 36.4513, 10.7357),
            new PlaceResult("Hammamet, Tunisie", 36.4000, 10.6167),
            new PlaceResult("Sousse, Tunisie", 35.8256, 10.6369),
            new PlaceResult("Monastir, Tunisie", 35.7643, 10.8113),
            new PlaceResult("Mahdia, Tunisie", 35.5047, 11.0622),
            new PlaceResult("Kairouan, Tunisie", 35.6781, 10.0963),
            new PlaceResult("Sfax, Tunisie", 34.7406, 10.7603),
            new PlaceResult("Gabès, Tunisie", 33.8815, 10.0982),
            new PlaceResult("Gafsa, Tunisie", 34.4250, 8.7842),
            new PlaceResult("Béja, Tunisie", 36.7256, 9.1817),
            new PlaceResult("Jendouba, Tunisie", 36.5011, 8.7802),
            new PlaceResult("Kasserine, Tunisie", 35.1676, 8.8365),
            new PlaceResult("Zaghouan, Tunisie", 36.4029, 10.1429));

    private Gazetteer() {
    }

    static List<PlaceResult> search(String text) {
        String q = GeoUtils.normalize(text);
        return PLACES.stream()
                .filter(p -> GeoUtils.normalize(p.label()).contains(q))
                .limit(6)
                .toList();
    }
}

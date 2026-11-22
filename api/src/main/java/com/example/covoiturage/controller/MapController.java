package com.example.covoiturage.controller;

import com.example.covoiturage.dto.PlaceResult;
import com.example.covoiturage.dto.RouteEstimateResponse;
import com.example.covoiturage.exception.BadRequestException;
import com.example.covoiturage.service.MapService;
import com.example.covoiturage.service.PricingService;
import com.example.covoiturage.util.GeoUtils;
import com.example.covoiturage.util.RouteCodec;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/maps")
@RequiredArgsConstructor
public class MapController {

    private final MapService mapService;
    private final PricingService pricingService;

    /** F09 : autocomplétion d'adresses (la clé API reste côté serveur). */
    @GetMapping("/geocode")
    public List<PlaceResult> geocode(@RequestParam("query") String query) {
        return mapService.autocomplete(query);
    }

    /** F10, F16 : itinéraire, distance, durée et prix suggéré. */
    @GetMapping("/route")
    public RouteEstimateResponse route(@RequestParam double fromLat, @RequestParam double fromLng,
                                       @RequestParam double toLat, @RequestParam double toLng,
                                       @RequestParam(defaultValue = "3") int seats) {
        if (seats < 1 || seats > 8) {
            throw new BadRequestException("Le nombre de places doit être entre 1 et 8");
        }
        if (GeoUtils.haversineKm(fromLat, fromLng, toLat, toLng) < 0.1) {
            throw new BadRequestException("Le départ et l'arrivée doivent être différents");
        }
        MapService.RouteResult r = mapService.route(fromLat, fromLng, toLat, toLng);
        return new RouteEstimateResponse(
                r.distanceKm(), r.durationMin(), RouteCodec.decode(RouteCodec.encode(r.points())),
                pricingService.suggestedPrice(r.distanceKm(), seats),
                pricingService.maxPrice(r.distanceKm(), seats),
                r.source());
    }
}

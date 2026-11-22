package com.example.mapservice.controller;

import com.example.mapservice.dto.PlaceResult;
import com.example.mapservice.dto.RouteResponse;
import com.example.mapservice.service.RoutingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
public class MapController {

    private final RoutingService routingService;

    public MapController(RoutingService routingService) {
        this.routingService = routingService;
    }

    /** Autocomplétion d'adresses. */
    @GetMapping("/geocode")
    public List<PlaceResult> geocode(@RequestParam("query") String query) {
        return routingService.autocomplete(query);
    }

    /** Itinéraire entre deux points : distance, durée et tracé. */
    @GetMapping("/route")
    public RouteResponse route(@RequestParam("fromLat") double fromLat, @RequestParam("fromLng") double fromLng,
                               @RequestParam("toLat") double toLat, @RequestParam("toLng") double toLng) {
        RoutingService.RouteResult r = routingService.route(fromLat, fromLng, toLat, toLng);
        List<List<Double>> points = new ArrayList<>();
        for (double[] p : r.points()) {
            points.add(List.of(p[0], p[1]));
        }
        return new RouteResponse(r.distanceKm(), r.durationMin(), points, r.source());
    }
}

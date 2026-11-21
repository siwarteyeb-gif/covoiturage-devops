package com.example.covoiturage.controller;

import com.example.covoiturage.dto.*;
import com.example.covoiturage.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

    /** Publie un trajet (ou une série de trajets récurrents) : renvoie la liste des trajets créés. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public List<TripResponse> create(Authentication auth, @Valid @RequestBody TripRequest request) {
        return tripService.create(auth.getName(), request);
    }

    @GetMapping("/search")
    public List<TripSearchResult> search(
            Authentication auth,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) Double fromLat,
            @RequestParam(required = false) Double fromLng,
            @RequestParam(required = false) Double toLat,
            @RequestParam(required = false) Double toLng,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Integer seats,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Double maxDetourKm,
            @RequestParam(required = false) String sort) {
        return tripService.search(auth.getName(), new TripSearchCriteria(
                from, to, fromLat, fromLng, toLat, toLng, date, seats, maxPrice, maxDetourKm, sort));
    }

    @GetMapping("/me")
    public List<TripResponse> mine(Authentication auth) {
        return tripService.myTrips(auth.getName());
    }

    @GetMapping("/{id}")
    public TripResponse get(@PathVariable Long id) {
        return tripService.getById(id);
    }

    @PutMapping("/{id}")
    public TripResponse update(Authentication auth, @PathVariable Long id,
                               @Valid @RequestBody TripUpdateRequest request) {
        return tripService.update(auth.getName(), id, request);
    }

    @PatchMapping("/{id}/status")
    public TripResponse changeStatus(Authentication auth, @PathVariable Long id,
                                     @Valid @RequestBody TripStatusRequest request) {
        return tripService.changeStatus(auth.getName(), id, request.status());
    }
}

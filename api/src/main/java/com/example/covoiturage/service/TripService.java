package com.example.covoiturage.service;

import com.example.covoiturage.dto.*;
import com.example.covoiturage.entity.*;
import com.example.covoiturage.exception.BadRequestException;
import com.example.covoiturage.exception.ForbiddenOperationException;
import com.example.covoiturage.exception.ResourceNotFoundException;
import com.example.covoiturage.repository.BookingRepository;
import com.example.covoiturage.repository.TripRepository;
import com.example.covoiturage.repository.UserRepository;
import com.example.covoiturage.repository.VehicleRepository;
import com.example.covoiturage.util.GeoUtils;
import com.example.covoiturage.util.RouteCodec;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TripService {

    private static final int MAX_SERIES = 30;
    private static final int MAX_RESULTS = 50;

    private final TripRepository tripRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final MapService mapService;
    private final PricingService pricingService;
    private final EmailService emailService;

    @Value("${search.default-max-detour-km:5}")
    private double defaultMaxDetourKm;

    // ------------------------------------------------------------------ F08, F10, F15 : publier

    @Transactional
    public List<TripResponse> create(String email, TripRequest req) {
        User driver = findUser(email);
        Vehicle vehicle = vehicleRepository.findByIdAndOwnerId(req.vehicleId(), driver.getId())
                .orElseThrow(() -> new BadRequestException("Véhicule introuvable : ajoutez d'abord un véhicule"));

        LocalDateTime now = LocalDateTime.now();
        if (!req.departureDateTime().isAfter(now)) {                          // RG4
            throw new BadRequestException("La date de départ doit être dans le futur");
        }
        int seats = req.totalSeats();
        if (seats > vehicle.getSeats()) {
            throw new BadRequestException("Ce véhicule ne dispose que de " + vehicle.getSeats() + " place(s)");
        }
        if (GeoUtils.haversineKm(req.departureLat(), req.departureLng(), req.arrivalLat(), req.arrivalLng()) < 0.1) {
            throw new BadRequestException("Le départ et l'arrivée doivent être différents");
        }

        // La distance et la durée sont calculées par le serveur : on ne fait pas confiance au client.
        MapService.RouteResult route = mapService.route(
                req.departureLat(), req.departureLng(), req.arrivalLat(), req.arrivalLng());

        BigDecimal max = pricingService.maxPrice(route.distanceKm(), seats);   // RG8
        if (req.pricePerSeat().compareTo(max) > 0) {
            throw new BadRequestException(
                    "Le prix dépasse le maximum autorisé (partage des frais) : " + max + " TND");
        }

        List<LocalDateTime> dates = departureDates(req, now);
        String seriesId = dates.size() > 1 ? UUID.randomUUID().toString() : null;
        String geometry = RouteCodec.encode(route.points());

        List<TripResponse> created = new ArrayList<>();
        for (LocalDateTime date : dates) {
            Trip trip = Trip.builder()
                    .driver(driver)
                    .vehicle(vehicle)
                    .departureAddress(req.departureAddress().trim())
                    .departureLat(req.departureLat())
                    .departureLng(req.departureLng())
                    .arrivalAddress(req.arrivalAddress().trim())
                    .arrivalLat(req.arrivalLat())
                    .arrivalLng(req.arrivalLng())
                    .departureDateTime(date)
                    .totalSeats(seats)
                    .availableSeats(seats)
                    .pricePerSeat(req.pricePerSeat())
                    .distanceKm(route.distanceKm())
                    .durationMin(route.durationMin())
                    .routeGeometry(geometry)
                    .status(TripStatus.OPEN)
                    .seriesId(seriesId)
                    .build();
            created.add(TripResponse.from(tripRepository.save(trip), false));
        }
        return created;
    }

    private List<LocalDateTime> departureDates(TripRequest req, LocalDateTime now) {
        Set<LocalDateTime> dates = new TreeSet<>();
        dates.add(req.departureDateTime());
        int weeks = req.recurrenceWeeks() == null ? 0 : req.recurrenceWeeks();
        List<Integer> weekdays = req.recurrenceWeekdays() == null ? List.of() : req.recurrenceWeekdays();
        if (weeks > 0 && !weekdays.isEmpty()) {
            for (int i = 1; i <= weeks * 7; i++) {
                LocalDateTime d = req.departureDateTime().plusDays(i);
                if (weekdays.contains(d.getDayOfWeek().getValue()) && d.isAfter(now)) {
                    dates.add(d);
                }
            }
        }
        if (dates.size() > MAX_SERIES) {
            throw new BadRequestException("Trop de trajets récurrents (maximum " + MAX_SERIES + ")");
        }
        return new ArrayList<>(dates);
    }

    // ------------------------------------------------------------------ F12, F13, F14 : rechercher

    @Transactional(readOnly = true)
    public List<TripSearchResult> search(String email, TripSearchCriteria c) {
        User user = findUser(email);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now;
        LocalDateTime end = now.plusYears(1);
        if (c.date() != null) {
            LocalDateTime dayStart = c.date().atStartOfDay();
            start = dayStart.isBefore(now) ? now : dayStart;
            end = dayStart.plusDays(1);
        }
        int seats = (c.seats() == null || c.seats() < 1) ? 1 : c.seats();

        List<Trip> trips = tripRepository.search(TripStatus.OPEN, seats, start, end, user.getId());

        boolean byCoordinates = c.fromLat() != null && c.fromLng() != null && c.toLat() != null && c.toLng() != null;
        double maxDetour = c.maxDetourKm() == null ? defaultMaxDetourKm : c.maxDetourKm();
        String from = GeoUtils.normalize(c.from());
        String to = GeoUtils.normalize(c.to());

        List<TripSearchResult> results = new ArrayList<>();
        for (Trip t : trips) {
            if (c.maxPrice() != null && t.getPricePerSeat().compareTo(c.maxPrice()) > 0) {
                continue;
            }
            Double detour = null;
            if (byCoordinates) {                                              // F14 : matching par détour
                double d = GeoUtils.detourKm(t.getDepartureLat(), t.getDepartureLng(),
                        t.getArrivalLat(), t.getArrivalLng(),
                        c.fromLat(), c.fromLng(), c.toLat(), c.toLng());
                detour = Math.round(d * 10.0) / 10.0;
                if (detour > maxDetour) {
                    continue;
                }
            } else {                                                          // recherche par texte
                if (!from.isEmpty() && !GeoUtils.normalize(t.getDepartureAddress()).contains(from)) {
                    continue;
                }
                if (!to.isEmpty() && !GeoUtils.normalize(t.getArrivalAddress()).contains(to)) {
                    continue;
                }
            }
            results.add(new TripSearchResult(TripResponse.from(t, false), detour));
            if (results.size() >= MAX_RESULTS) {
                break;
            }
        }
        sort(results, c.sort());
        return results;
    }

    private void sort(List<TripSearchResult> results, String sort) {
        if (sort == null) {
            return;                                                           // déjà triés par heure
        }
        switch (sort) {
            case "price" -> results.sort(Comparator.comparing((TripSearchResult r) -> r.trip().pricePerSeat()));
            case "rating" -> results.sort(Comparator.comparingDouble(
                    (TripSearchResult r) -> r.trip().driver().averageRating()).reversed());
            case "detour" -> results.sort(Comparator.comparing(
                    TripSearchResult::detourKm, Comparator.nullsLast(Comparator.naturalOrder())));
            default -> {
            }
        }
    }

    // ------------------------------------------------------------------ consultation

    @Transactional(readOnly = true)
    public TripResponse getById(Long id) {
        Trip trip = tripRepository.findDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trajet introuvable"));
        return TripResponse.from(trip, true);
    }

    @Transactional(readOnly = true)
    public List<TripResponse> myTrips(String email) {
        User driver = findUser(email);
        return tripRepository.findByDriverIdOrderByDepartureDateTimeDesc(driver.getId())
                .stream().map(t -> TripResponse.from(t, false)).toList();
    }

    // ------------------------------------------------------------------ F11 : modifier

    @Transactional
    public TripResponse update(String email, Long id, TripUpdateRequest req) {
        User driver = findUser(email);
        Trip trip = lockTrip(id);
        ensureOwner(trip, driver);
        if (trip.getStatus() != TripStatus.OPEN && trip.getStatus() != TripStatus.FULL) {
            throw new BadRequestException("Ce trajet ne peut plus être modifié");
        }
        if (!req.departureDateTime().isAfter(LocalDateTime.now())) {          // RG4
            throw new BadRequestException("La date de départ doit être dans le futur");
        }
        int total = req.totalSeats();
        if (total > trip.getVehicle().getSeats()) {
            throw new BadRequestException("Ce véhicule ne dispose que de " + trip.getVehicle().getSeats() + " place(s)");
        }
        int accepted = acceptedSeats(trip.getId());
        if (total < accepted) {
            throw new BadRequestException("Impossible : " + accepted + " place(s) sont déjà acceptées");
        }
        BigDecimal max = pricingService.maxPrice(trip.getDistanceKm(), total);
        if (req.pricePerSeat().compareTo(max) > 0) {
            throw new BadRequestException("Le prix dépasse le maximum autorisé (partage des frais) : " + max + " TND");
        }
        trip.setDepartureDateTime(req.departureDateTime());
        trip.setTotalSeats(total);
        trip.setAvailableSeats(total - accepted);
        trip.setPricePerSeat(req.pricePerSeat());
        trip.setStatus(trip.getAvailableSeats() == 0 ? TripStatus.FULL : TripStatus.OPEN);
        return TripResponse.from(tripRepository.save(trip), true);
    }

    // ------------------------------------------------------------------ F11, F22 : statut

    @Transactional
    public TripResponse changeStatus(String email, Long id, TripStatus target) {
        User driver = findUser(email);
        Trip trip = lockTrip(id);
        ensureOwner(trip, driver);
        TripStatus current = trip.getStatus();
        boolean open = current == TripStatus.OPEN || current == TripStatus.FULL;

        if (target == TripStatus.CANCELLED && open) {
            for (Booking b : bookingRepository.findByTripIdAndStatusIn(id,
                    List.of(BookingStatus.PENDING, BookingStatus.ACCEPTED))) {
                b.setStatus(BookingStatus.CANCELLED);
                bookingRepository.save(b);
                emailService.send(b.getPassenger().getEmail(), "Trajet annulé",
                        "Le trajet " + trip.getDepartureAddress() + " → " + trip.getArrivalAddress()
                                + " du " + trip.getDepartureDateTime() + " a été annulé par le conducteur.");
            }
        } else if (target == TripStatus.IN_PROGRESS && open) {
            for (Booking b : bookingRepository.findByTripIdAndStatusIn(id, List.of(BookingStatus.PENDING))) {
                b.setStatus(BookingStatus.REJECTED);                          // plus d'acceptation possible
                bookingRepository.save(b);
            }
        } else if (target == TripStatus.DONE && current == TripStatus.IN_PROGRESS) {
            // rien d'autre à faire : les avis deviennent possibles (RG7)
        } else {
            throw new BadRequestException("Changement de statut impossible : " + current + " → " + target);
        }
        trip.setStatus(target);
        return TripResponse.from(tripRepository.save(trip), false);
    }

    // ------------------------------------------------------------------ outils

    private int acceptedSeats(Long tripId) {
        return bookingRepository.findByTripIdAndStatusIn(tripId, List.of(BookingStatus.ACCEPTED))
                .stream().mapToInt(Booking::getSeats).sum();
    }

    private Trip lockTrip(Long id) {
        return tripRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trajet introuvable"));
    }

    private void ensureOwner(Trip trip, User driver) {
        if (!trip.getDriver().getId().equals(driver.getId())) {
            throw new ForbiddenOperationException("Ce trajet ne vous appartient pas");
        }
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
    }
}

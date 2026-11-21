package com.example.covoiturage.service;

import com.example.covoiturage.dto.BookingRequest;
import com.example.covoiturage.dto.BookingResponse;
import com.example.covoiturage.entity.*;
import com.example.covoiturage.exception.BadRequestException;
import com.example.covoiturage.exception.ConflictException;
import com.example.covoiturage.exception.ForbiddenOperationException;
import com.example.covoiturage.exception.ResourceNotFoundException;
import com.example.covoiturage.repository.BookingRepository;
import com.example.covoiturage.repository.ReviewRepository;
import com.example.covoiturage.repository.TripRepository;
import com.example.covoiturage.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Réservations. Chaque opération qui touche aux places verrouille d'abord la ligne du trajet
 * (verrou pessimiste) : deux personnes ne peuvent donc jamais prendre la dernière place en même temps.
 */
@Service
@RequiredArgsConstructor
public class BookingService {

    private static final List<BookingStatus> ACTIVE = List.of(BookingStatus.PENDING, BookingStatus.ACCEPTED);

    private final BookingRepository bookingRepository;
    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final EmailService emailService;

    // ------------------------------------------------------------------  : demander

    @Transactional
    public BookingResponse request(String email, Long tripId, BookingRequest req) {
        User passenger = findUser(email);
        Trip trip = lockTrip(tripId);

        if (trip.getDriver().getId().equals(passenger.getId())) {                       // RG2
            throw new BadRequestException("Vous ne pouvez pas réserver votre propre trajet");
        }
        if (trip.getStatus() != TripStatus.OPEN) {
            throw new BadRequestException("Ce trajet n'accepte plus de réservations");
        }
        if (!trip.getDepartureDateTime().isAfter(LocalDateTime.now())) {                // RG4
            throw new BadRequestException("Ce trajet est déjà passé");
        }
        if (req.seats() > trip.getAvailableSeats()) {                                   // RG1
            throw new BadRequestException("Pas assez de places disponibles (" + trip.getAvailableSeats() + " restante(s))");
        }
        if (bookingRepository.existsByTripIdAndPassengerIdAndStatusIn(tripId, passenger.getId(), ACTIVE)) { // RG3
            throw new ConflictException("Vous avez déjà une réservation en cours pour ce trajet");
        }

        Booking booking = bookingRepository.save(Booking.builder()
                .trip(trip)
                .passenger(passenger)
                .seats(req.seats())
                .status(BookingStatus.PENDING)
                .build());

        emailService.send(trip.getDriver().getEmail(), "Nouvelle demande de réservation",
                passenger.getName() + " demande " + req.seats() + " place(s) pour votre trajet "
                        + trip.getDepartureAddress() + " → " + trip.getArrivalAddress() + ".");
        return toResponse(booking, passenger.getId());
    }

    // ------------------------------------------------------------------ F18 : accepter / refuser

    @Transactional
    public BookingResponse accept(String email, Long bookingId) {
        User driver = findUser(email);
        Trip trip = lockTripOfBooking(bookingId);
        Booking booking = findBooking(bookingId);
        ensureDriver(trip, driver);

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BadRequestException("Seules les demandes en attente peuvent être acceptées");
        }
        if (trip.getStatus() != TripStatus.OPEN) {
            throw new BadRequestException("Ce trajet n'accepte plus de réservations");
        }
        if (booking.getSeats() > trip.getAvailableSeats()) {                            // RG1
            throw new ConflictException("Pas assez de places restantes pour accepter cette demande");
        }
        trip.setAvailableSeats(trip.getAvailableSeats() - booking.getSeats());          // RG5
        if (trip.getAvailableSeats() == 0) {
            trip.setStatus(TripStatus.FULL);                                            // RG6
        }
        booking.setStatus(BookingStatus.ACCEPTED);
        tripRepository.save(trip);
        bookingRepository.save(booking);

        emailService.send(booking.getPassenger().getEmail(), "Réservation acceptée",
                "Votre réservation pour " + trip.getDepartureAddress() + " → " + trip.getArrivalAddress()
                        + " (" + trip.getDepartureDateTime() + ") a été acceptée.");
        return toResponse(booking, driver.getId());
    }

    @Transactional
    public BookingResponse reject(String email, Long bookingId) {
        User driver = findUser(email);
        Trip trip = lockTripOfBooking(bookingId);
        Booking booking = findBooking(bookingId);
        ensureDriver(trip, driver);

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BadRequestException("Seules les demandes en attente peuvent être refusées");
        }
        booking.setStatus(BookingStatus.REJECTED);
        bookingRepository.save(booking);

        emailService.send(booking.getPassenger().getEmail(), "Réservation refusée",
                "Votre demande pour " + trip.getDepartureAddress() + " → " + trip.getArrivalAddress() + " a été refusée.");
        return toResponse(booking, driver.getId());
    }

    // ------------------------------------------------------------------ F19 : annuler

    @Transactional
    public BookingResponse cancel(String email, Long bookingId) {
        User user = findUser(email);
        Trip trip = lockTripOfBooking(bookingId);
        Booking booking = findBooking(bookingId);

        boolean isPassenger = booking.getPassenger().getId().equals(user.getId());
        boolean isDriver = trip.getDriver().getId().equals(user.getId());
        if (!isPassenger && !isDriver) {
            throw new ForbiddenOperationException("Cette réservation ne vous concerne pas");
        }
        if (!ACTIVE.contains(booking.getStatus())) {
            throw new BadRequestException("Cette réservation ne peut plus être annulée");
        }
        if (trip.getStatus() == TripStatus.IN_PROGRESS || trip.getStatus() == TripStatus.DONE) {
            throw new BadRequestException("Le trajet a déjà commencé");
        }
        if (booking.getStatus() == BookingStatus.ACCEPTED) {                            // RG5 : on rend les places
            trip.setAvailableSeats(trip.getAvailableSeats() + booking.getSeats());
            if (trip.getStatus() == TripStatus.FULL) {
                trip.setStatus(TripStatus.OPEN);
            }
            tripRepository.save(trip);
        }
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        User other = isPassenger ? trip.getDriver() : booking.getPassenger();
        emailService.send(other.getEmail(), "Réservation annulée",
                user.getName() + " a annulé la réservation pour " + trip.getDepartureAddress()
                        + " → " + trip.getArrivalAddress() + ".");
        return toResponse(booking, user.getId());
    }

    // ------------------------------------------------------------------ F21 : consulter

    @Transactional(readOnly = true)
    public List<BookingResponse> myBookings(String email) {
        User user = findUser(email);
        return bookingRepository.findByPassengerIdOrderByCreatedAtDesc(user.getId())
                .stream().map(b -> toResponse(b, user.getId())).toList();
    }

    /** Demandes reçues sur tous les trajets du conducteur. */
    @Transactional(readOnly = true)
    public List<BookingResponse> receivedRequests(String email) {
        User driver = findUser(email);
        return bookingRepository.findByTripDriverIdOrderByCreatedAtDesc(driver.getId())
                .stream().map(b -> toResponse(b, driver.getId())).toList();
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> tripBookings(String email, Long tripId) {
        User driver = findUser(email);
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trajet introuvable"));
        ensureDriver(trip, driver);
        return bookingRepository.findByTripIdOrderByCreatedAtAsc(tripId)
                .stream().map(b -> toResponse(b, driver.getId())).toList();
    }

    // ------------------------------------------------------------------ outils

    private BookingResponse toResponse(Booking b, Long currentUserId) {
        return BookingResponse.from(b, reviewRepository.existsByBookingIdAndAuthorId(b.getId(), currentUserId));
    }

    private Trip lockTrip(Long tripId) {
        return tripRepository.findByIdForUpdate(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trajet introuvable"));
    }

    /** On verrouille le trajet AVANT de charger la réservation, pour lire des données à jour. */
    private Trip lockTripOfBooking(Long bookingId) {
        Long tripId = bookingRepository.findTripIdById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation introuvable"));
        return lockTrip(tripId);
    }

    private Booking findBooking(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation introuvable"));
    }

    private void ensureDriver(Trip trip, User user) {
        if (!trip.getDriver().getId().equals(user.getId())) {
            throw new ForbiddenOperationException("Seul le conducteur de ce trajet peut faire cette action");
        }
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
    }
}

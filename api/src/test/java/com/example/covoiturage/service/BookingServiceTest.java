package com.example.covoiturage.service;

import com.example.covoiturage.dto.BookingRequest;
import com.example.covoiturage.dto.BookingResponse;
import com.example.covoiturage.entity.*;
import com.example.covoiturage.exception.BadRequestException;
import com.example.covoiturage.exception.ConflictException;
import com.example.covoiturage.repository.BookingRepository;
import com.example.covoiturage.repository.ReviewRepository;
import com.example.covoiturage.repository.TripRepository;
import com.example.covoiturage.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private TripRepository tripRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private BookingService bookingService;

    private User driver;
    private User passenger;
    private Trip trip;

    @BeforeEach
    void setUp() {
        driver = User.builder().id(1L).name("Karim").email("karim@test.tn").role(Role.USER).build();
        passenger = User.builder().id(2L).name("Salma").email("salma@test.tn").role(Role.USER).build();
        trip = Trip.builder().id(10L).driver(driver)
                .departureAddress("Ariana").arrivalAddress("Manouba")
                .departureDateTime(LocalDateTime.now().plusDays(1))
                .totalSeats(3).availableSeats(3)
                .pricePerSeat(new BigDecimal("1.20"))
                .status(TripStatus.OPEN).build();
    }

    private Booking booking(BookingStatus status, int seats) {
        return Booking.builder().id(5L).trip(trip).passenger(passenger).seats(seats).status(status).build();
    }

    @Test
    void driverCannotBookHisOwnTrip() {                                                   // RG2
        when(userRepository.findByEmail("karim@test.tn")).thenReturn(Optional.of(driver));
        when(tripRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(trip));

        assertThrows(BadRequestException.class,
                () -> bookingService.request("karim@test.tn", 10L, new BookingRequest(1)));
    }

    @Test
    void cannotBookMoreSeatsThanAvailable() {                                             // RG1
        when(userRepository.findByEmail("salma@test.tn")).thenReturn(Optional.of(passenger));
        when(tripRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(trip));

        assertThrows(BadRequestException.class,
                () -> bookingService.request("salma@test.tn", 10L, new BookingRequest(4)));
    }

    @Test
    void cannotBookTheSameTripTwice() {                                                   // RG3
        when(userRepository.findByEmail("salma@test.tn")).thenReturn(Optional.of(passenger));
        when(tripRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(trip));
        when(bookingRepository.existsByTripIdAndPassengerIdAndStatusIn(eq(10L), eq(2L), any(Collection.class)))
                .thenReturn(true);

        assertThrows(ConflictException.class,
                () -> bookingService.request("salma@test.tn", 10L, new BookingRequest(1)));
    }

    @Test
    void cannotBookAPastTrip() {                                                          // RG4
        trip.setDepartureDateTime(LocalDateTime.now().minusHours(1));
        when(userRepository.findByEmail("salma@test.tn")).thenReturn(Optional.of(passenger));
        when(tripRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(trip));

        assertThrows(BadRequestException.class,
                () -> bookingService.request("salma@test.tn", 10L, new BookingRequest(1)));
    }

    @Test
    void validRequestCreatesAPendingBooking() {
        when(userRepository.findByEmail("salma@test.tn")).thenReturn(Optional.of(passenger));
        when(tripRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(trip));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingResponse response = bookingService.request("salma@test.tn", 10L, new BookingRequest(2));

        assertEquals(BookingStatus.PENDING, response.status());
        assertEquals(2, response.seats());
        assertEquals(3, trip.getAvailableSeats());      // les places ne baissent qu'à l'acceptation
    }

    @Test
    void acceptingDecreasesSeatsAndMarksTheTripFullWhenNoSeatLeft() {                     // RG5, RG6
        trip.setAvailableSeats(2);
        Booking pending = booking(BookingStatus.PENDING, 2);
        when(userRepository.findByEmail("karim@test.tn")).thenReturn(Optional.of(driver));
        when(bookingRepository.findTripIdById(5L)).thenReturn(Optional.of(10L));
        when(tripRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(trip));
        when(bookingRepository.findById(5L)).thenReturn(Optional.of(pending));

        BookingResponse response = bookingService.accept("karim@test.tn", 5L);

        assertEquals(BookingStatus.ACCEPTED, response.status());
        assertEquals(0, trip.getAvailableSeats());
        assertEquals(TripStatus.FULL, trip.getStatus());
    }

    @Test
    void acceptingFailsWhenNotEnoughSeatsRemain() {                                       // RG1
        trip.setAvailableSeats(1);
        Booking pending = booking(BookingStatus.PENDING, 2);
        when(userRepository.findByEmail("karim@test.tn")).thenReturn(Optional.of(driver));
        when(bookingRepository.findTripIdById(5L)).thenReturn(Optional.of(10L));
        when(tripRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(trip));
        when(bookingRepository.findById(5L)).thenReturn(Optional.of(pending));

        assertThrows(ConflictException.class, () -> bookingService.accept("karim@test.tn", 5L));
    }

    @Test
    void cancellingAnAcceptedBookingGivesTheSeatsBackAndReopensTheTrip() {                // RG5
        trip.setAvailableSeats(0);
        trip.setStatus(TripStatus.FULL);
        Booking accepted = booking(BookingStatus.ACCEPTED, 2);
        when(userRepository.findByEmail("salma@test.tn")).thenReturn(Optional.of(passenger));
        when(bookingRepository.findTripIdById(5L)).thenReturn(Optional.of(10L));
        when(tripRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(trip));
        when(bookingRepository.findById(5L)).thenReturn(Optional.of(accepted));

        BookingResponse response = bookingService.cancel("salma@test.tn", 5L);

        assertEquals(BookingStatus.CANCELLED, response.status());
        assertEquals(2, trip.getAvailableSeats());
        assertEquals(TripStatus.OPEN, trip.getStatus());
    }
}

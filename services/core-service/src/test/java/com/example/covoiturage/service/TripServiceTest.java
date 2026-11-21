package com.example.covoiturage.service;

import com.example.covoiturage.dto.TripRequest;
import com.example.covoiturage.dto.TripResponse;
import com.example.covoiturage.entity.Role;
import com.example.covoiturage.entity.Trip;
import com.example.covoiturage.entity.User;
import com.example.covoiturage.entity.Vehicle;
import com.example.covoiturage.exception.BadRequestException;
import com.example.covoiturage.repository.BookingRepository;
import com.example.covoiturage.repository.TripRepository;
import com.example.covoiturage.repository.UserRepository;
import com.example.covoiturage.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TripServiceTest {

    @Mock
    private TripRepository tripRepository;
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private EmailService emailService;

    private TripService tripService;
    private User driver;
    private Vehicle vehicle;

    @BeforeEach
    void setUp() {
        tripService = new TripService(tripRepository, vehicleRepository, userRepository, bookingRepository,
                new MapService("", "http://localhost:1"), new PricingService(0.30, 1.5), emailService);
        driver = User.builder().id(1L).name("Karim").email("karim@test.tn").role(Role.USER).build();
        vehicle = Vehicle.builder().id(7L).brand("Peugeot").model("208").seats(4).owner(driver).build();
        when(userRepository.findByEmail("karim@test.tn")).thenReturn(Optional.of(driver));
        when(vehicleRepository.findByIdAndOwnerId(7L, 1L)).thenReturn(Optional.of(vehicle));
    }

    private TripRequest request(LocalDateTime date, int seats, String price) {
        // Ariana -> La Manouba
        return new TripRequest(7L, "Ariana", 36.8665, 10.1647, "La Manouba", 36.8101, 10.0863,
                date, seats, new BigDecimal(price), null, null);
    }

    @Test
    void cannotPublishATripInThePast() {                                                  // RG4
        TripRequest req = request(LocalDateTime.now().minusHours(2), 3, "1.00");
        assertThrows(BadRequestException.class, () -> tripService.create("karim@test.tn", req));
    }

    @Test
    void cannotOfferMoreSeatsThanTheVehicleHas() {
        TripRequest req = request(LocalDateTime.now().plusDays(1), 5, "1.00");
        assertThrows(BadRequestException.class, () -> tripService.create("karim@test.tn", req));
    }

    @Test
    void priceAboveTheCapIsRejected() {                                                   // RG8
        TripRequest req = request(LocalDateTime.now().plusDays(1), 3, "50.00");
        assertThrows(BadRequestException.class, () -> tripService.create("karim@test.tn", req));
    }

    @Test
    void validTripIsCreatedOpenWithAllSeatsAvailable() {
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> inv.getArgument(0));
        TripRequest req = request(LocalDateTime.now().plusDays(1), 3, "1.00");

        List<TripResponse> created = tripService.create("karim@test.tn", req);

        assertEquals(1, created.size());
        assertEquals(3, created.get(0).availableSeats());
        assertEquals("OPEN", created.get(0).status().name());
        assertTrue(created.get(0).distanceKm() > 0);
    }

    @Test
    void recurrentTripsCreateOneTripPerSelectedWeekday() {                                // F15
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> inv.getArgument(0));
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        TripRequest req = new TripRequest(7L, "Ariana", 36.8665, 10.1647, "La Manouba", 36.8101, 10.0863,
                start, 3, new BigDecimal("1.00"), List.of(1, 2, 3, 4, 5, 6, 7), 1);

        List<TripResponse> created = tripService.create("karim@test.tn", req);

        assertEquals(8, created.size());   // le jour de départ + chaque jour de la semaine suivante
        assertNotNull(created.get(0).seriesId());
    }
}

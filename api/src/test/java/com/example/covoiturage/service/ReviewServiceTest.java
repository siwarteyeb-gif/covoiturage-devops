package com.example.covoiturage.service;

import com.example.covoiturage.dto.ReviewRequest;
import com.example.covoiturage.dto.ReviewResponse;
import com.example.covoiturage.entity.*;
import com.example.covoiturage.exception.BadRequestException;
import com.example.covoiturage.exception.ForbiddenOperationException;
import com.example.covoiturage.repository.BookingRepository;
import com.example.covoiturage.repository.ReviewRepository;
import com.example.covoiturage.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReviewService reviewService;

    private User driver;
    private User passenger;
    private Trip trip;
    private Booking booking;

    @BeforeEach
    void setUp() {
        driver = User.builder().id(1L).name("Karim").email("karim@test.tn").role(Role.USER).build();
        passenger = User.builder().id(2L).name("Salma").email("salma@test.tn").role(Role.USER).build();
        trip = Trip.builder().id(10L).driver(driver).status(TripStatus.DONE).build();
        booking = Booking.builder().id(5L).trip(trip).passenger(passenger).seats(1)
                .status(BookingStatus.ACCEPTED).build();
    }

    @Test
    void reviewIsRefusedBeforeTheTripIsDone() {                                           // RG7
        trip.setStatus(TripStatus.OPEN);
        when(userRepository.findByEmail("salma@test.tn")).thenReturn(Optional.of(passenger));
        when(bookingRepository.findById(5L)).thenReturn(Optional.of(booking));

        assertThrows(BadRequestException.class,
                () -> reviewService.create("salma@test.tn", 5L, new ReviewRequest(5, "Super")));
    }

    @Test
    void someoneWhoDidNotTakePartCannotReview() {
        User stranger = User.builder().id(99L).name("Autre").email("autre@test.tn").role(Role.USER).build();
        when(userRepository.findByEmail("autre@test.tn")).thenReturn(Optional.of(stranger));
        when(bookingRepository.findById(5L)).thenReturn(Optional.of(booking));

        assertThrows(ForbiddenOperationException.class,
                () -> reviewService.create("autre@test.tn", 5L, new ReviewRequest(3, null)));
    }

    @Test
    void passengerReviewsTheDriverAndTheAverageIsUpdated() {                              // F24, F25
        when(userRepository.findByEmail("salma@test.tn")).thenReturn(Optional.of(passenger));
        when(bookingRepository.findById(5L)).thenReturn(Optional.of(booking));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reviewRepository.averageRatingOf(1L)).thenReturn(4.5);
        when(userRepository.findById(1L)).thenReturn(Optional.of(driver));

        ReviewResponse response = reviewService.create("salma@test.tn", 5L, new ReviewRequest(5, " Excellent "));

        assertEquals(1L, response.target().id());
        assertEquals("Excellent", response.comment());
        assertEquals(4.5, driver.getAverageRating(), 0.0001);
    }
}

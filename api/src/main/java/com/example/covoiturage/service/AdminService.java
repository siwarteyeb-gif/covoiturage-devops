package com.example.covoiturage.service;

import com.example.covoiturage.dto.AdminUserResponse;
import com.example.covoiturage.dto.ReviewResponse;
import com.example.covoiturage.dto.StatsResponse;
import com.example.covoiturage.dto.TripResponse;
import com.example.covoiturage.entity.BookingStatus;
import com.example.covoiturage.entity.Review;
import com.example.covoiturage.entity.Role;
import com.example.covoiturage.entity.TripStatus;
import com.example.covoiturage.entity.User;
import com.example.covoiturage.exception.BadRequestException;
import com.example.covoiturage.exception.ResourceNotFoundException;
import com.example.covoiturage.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final TripRepository tripRepository;
    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewService reviewService;

    // ------------------------------------------------------------------ F26 : utilisateurs

    @Transactional(readOnly = true)
    public List<AdminUserResponse> users() {
        return userRepository.findAll(Sort.by("id")).stream().map(AdminUserResponse::from).toList();
    }

    @Transactional
    public AdminUserResponse setBlocked(Long userId, boolean blocked, String adminEmail) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        if (user.getRole() == Role.ADMIN || user.getEmail().equals(adminEmail)) {
            throw new BadRequestException("Un administrateur ne peut pas être bloqué");
        }
        user.setBlocked(blocked);                                                       // RG9
        return AdminUserResponse.from(userRepository.save(user));
    }

    // ------------------------------------------------------------------ F27 : modération

    @Transactional(readOnly = true)
    public List<TripResponse> trips() {
        return tripRepository.findAllByOrderByDepartureDateTimeDesc()
                .stream().map(t -> TripResponse.from(t, false)).toList();
    }

    @Transactional
    public void deleteTrip(Long tripId) {
        if (!tripRepository.existsById(tripId)) {
            throw new ResourceNotFoundException("Trajet introuvable");
        }
        Set<Long> affectedUsers = new LinkedHashSet<>();
        reviewRepository.findByBookingTripId(tripId).forEach(r -> affectedUsers.add(r.getTarget().getId()));
        reviewRepository.deleteByBookingTripId(tripId);
        bookingRepository.deleteByTripId(tripId);
        tripRepository.deleteById(tripId);
        tripRepository.flush();
        affectedUsers.forEach(reviewService::recomputeAverage);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> reviews() {
        return reviewRepository.findAllByOrderByCreatedAtDesc().stream().map(ReviewResponse::from).toList();
    }

    @Transactional
    public void deleteReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Avis introuvable"));
        Long targetId = review.getTarget().getId();
        reviewRepository.delete(review);
        reviewRepository.flush();
        reviewService.recomputeAverage(targetId);
    }

    // ------------------------------------------------------------------ F28 : statistiques

    @Transactional(readOnly = true)
    public StatsResponse stats() {
        long offered = tripRepository.sumOfferedSeats();
        long accepted = bookingRepository.sumAcceptedSeats();
        double fillRate = offered == 0 ? 0.0 : Math.round(accepted * 1000.0 / offered) / 10.0;

        List<StatsResponse.PopularRoute> popular = bookingRepository.popularRoutes(PageRequest.of(0, 5))
                .stream()
                .map(row -> new StatsResponse.PopularRoute(
                        String.valueOf(row[0]), String.valueOf(row[1]), ((Number) row[2]).longValue()))
                .toList();

        return new StatsResponse(
                userRepository.count(),
                vehicleRepository.countDrivers(),
                tripRepository.count(),
                tripRepository.countByStatus(TripStatus.OPEN),
                bookingRepository.count(),
                bookingRepository.countByStatus(BookingStatus.ACCEPTED),
                fillRate,
                popular);
    }
}

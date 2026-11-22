package com.example.covoiturage.service;

import com.example.covoiturage.dto.ReviewRequest;
import com.example.covoiturage.dto.ReviewResponse;
import com.example.covoiturage.entity.*;
import com.example.covoiturage.exception.BadRequestException;
import com.example.covoiturage.exception.ConflictException;
import com.example.covoiturage.exception.ForbiddenOperationException;
import com.example.covoiturage.exception.ResourceNotFoundException;
import com.example.covoiturage.repository.BookingRepository;
import com.example.covoiturage.repository.ReviewRepository;
import com.example.covoiturage.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    /** F24 : le passager note le conducteur, et le conducteur note le passager (RG7). */
    @Transactional
    public ReviewResponse create(String email, Long bookingId, ReviewRequest req) {
        User author = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation introuvable"));
        Trip trip = booking.getTrip();

        if (trip.getStatus() != TripStatus.DONE || booking.getStatus() != BookingStatus.ACCEPTED) {   // RG7
            throw new BadRequestException("Un avis n'est possible qu'après un trajet terminé");
        }

        User target;
        if (booking.getPassenger().getId().equals(author.getId())) {
            target = trip.getDriver();
        } else if (trip.getDriver().getId().equals(author.getId())) {
            target = booking.getPassenger();
        } else {
            throw new ForbiddenOperationException("Vous n'avez pas participé à ce trajet");
        }
        if (reviewRepository.existsByBookingIdAndAuthorId(bookingId, author.getId())) {               // RG7
            throw new ConflictException("Vous avez déjà laissé un avis pour ce trajet");
        }

        Review review = reviewRepository.save(Review.builder()
                .booking(booking)
                .author(author)
                .target(target)
                .rating(req.rating())
                .comment(req.comment() == null || req.comment().isBlank() ? null : req.comment().trim())
                .build());
        recomputeAverage(target.getId());
        return ReviewResponse.from(review);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> reviewsOf(Long userId) {
        return reviewRepository.findByTargetIdOrderByCreatedAtDesc(userId)
                .stream().map(ReviewResponse::from).toList();
    }

    /** F25 : met à jour la note moyenne affichée sur le profil. */
    @Transactional
    public void recomputeAverage(Long userId) {
        Double avg = reviewRepository.averageRatingOf(userId);
        userRepository.findById(userId).ifPresent(u -> {
            u.setAverageRating(avg == null ? 0.0 : Math.round(avg * 10.0) / 10.0);
            userRepository.save(u);
        });
    }
}

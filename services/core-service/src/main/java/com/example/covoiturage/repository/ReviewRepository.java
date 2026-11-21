package com.example.covoiturage.repository;

import com.example.covoiturage.entity.Review;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByBookingIdAndAuthorId(Long bookingId, Long authorId);

    @EntityGraph(attributePaths = {"author", "target"})
    List<Review> findByTargetIdOrderByCreatedAtDesc(Long targetId);

    @EntityGraph(attributePaths = {"author", "target"})
    List<Review> findAllByOrderByCreatedAtDesc();

    List<Review> findByBookingTripId(Long tripId);

    @Query("select avg(r.rating) from Review r where r.target.id = :userId")
    Double averageRatingOf(@Param("userId") Long userId);

    void deleteByBookingTripId(Long tripId);
}

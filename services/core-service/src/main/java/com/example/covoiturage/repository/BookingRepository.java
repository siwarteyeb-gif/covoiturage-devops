package com.example.covoiturage.repository;

import com.example.covoiturage.entity.Booking;
import com.example.covoiturage.entity.BookingStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("select b.trip.id from Booking b where b.id = :id")
    Optional<Long> findTripIdById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"trip", "trip.driver", "passenger"})
    List<Booking> findByPassengerIdOrderByCreatedAtDesc(Long passengerId);

    @EntityGraph(attributePaths = {"trip", "trip.driver", "passenger"})
    List<Booking> findByTripDriverIdOrderByCreatedAtDesc(Long driverId);

    @EntityGraph(attributePaths = {"trip", "trip.driver", "passenger"})
    List<Booking> findByTripIdOrderByCreatedAtAsc(Long tripId);

    List<Booking> findByTripIdAndStatusIn(Long tripId, Collection<BookingStatus> statuses);

    boolean existsByTripIdAndPassengerIdAndStatusIn(Long tripId, Long passengerId, Collection<BookingStatus> statuses);

    long countByStatus(BookingStatus status);

    @Query("select coalesce(sum(b.seats), 0) from Booking b where b.status = com.example.covoiturage.entity.BookingStatus.ACCEPTED")
    long sumAcceptedSeats();

    @Query("""
            select t.departureAddress, t.arrivalAddress, count(b)
            from Booking b join b.trip t
            group by t.departureAddress, t.arrivalAddress
            order by count(b) desc
            """)
    List<Object[]> popularRoutes(Pageable pageable);

    void deleteByTripId(Long tripId);
}

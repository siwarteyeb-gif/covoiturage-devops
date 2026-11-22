package com.example.covoiturage.repository;

import com.example.covoiturage.entity.Trip;
import com.example.covoiturage.entity.TripStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TripRepository extends JpaRepository<Trip, Long> {

    /** Verrou pessimiste : bloque la ligne du trajet pendant la transaction (anti-surréservation). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Trip t where t.id = :id")
    Optional<Trip> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {"driver", "vehicle"})
    @Query("select t from Trip t where t.id = :id")
    Optional<Trip> findDetailsById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"driver", "vehicle"})
    @Query("""
            select t from Trip t
            where t.status = :status
              and t.availableSeats >= :seats
              and t.departureDateTime >= :start
              and t.departureDateTime < :end
              and t.driver.id <> :excludedDriverId
            order by t.departureDateTime asc
            """)
    List<Trip> search(@Param("status") TripStatus status,
                      @Param("seats") int seats,
                      @Param("start") LocalDateTime start,
                      @Param("end") LocalDateTime end,
                      @Param("excludedDriverId") Long excludedDriverId);

    @EntityGraph(attributePaths = {"driver", "vehicle"})
    List<Trip> findByDriverIdOrderByDepartureDateTimeDesc(Long driverId);

    @EntityGraph(attributePaths = {"driver", "vehicle"})
    List<Trip> findAllByOrderByDepartureDateTimeDesc();

    long countByStatus(TripStatus status);

    @Query("select coalesce(sum(t.totalSeats), 0) from Trip t where t.status <> com.example.covoiturage.entity.TripStatus.CANCELLED")
    long sumOfferedSeats();
}

package com.example.covoiturage.repository;

import com.example.covoiturage.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findByOwnerIdOrderByIdDesc(Long ownerId);

    Optional<Vehicle> findByIdAndOwnerId(Long id, Long ownerId);

    @Query("select count(distinct v.owner.id) from Vehicle v")
    long countDrivers();
}

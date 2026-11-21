package com.example.covoiturage.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trips")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "driver_id", nullable = false)
    private User driver;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(nullable = false)
    private String departureAddress;
    //latitude du point de départ
    private double departureLat;
    private double departureLng;

    @Column(nullable = false)
    private String arrivalAddress;
    private double arrivalLat;
    private double arrivalLng;

    @Column(nullable = false)
    private LocalDateTime departureDateTime;

    private int totalSeats;

    private int availableSeats;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal pricePerSeat;

    private double distanceKm;

    private int durationMin;

    /** Tracé de l'itinéraire, format "lat,lng;lat,lng;..." (voir RouteCodec). */
    //indique que la colonne doit être de type TEXT dans la base de données, pour pouvoir stocker une longue chaîne de caractères
    @Column(columnDefinition = "TEXT")
    //représente le chemin complet entre le point de départ et le point d'arrivée
    private String routeGeometry;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TripStatus status;

    /** Identifiant commun aux trajets créés ensemble (trajets récurrents). */
    @Column(length = 40)
    private String seriesId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}

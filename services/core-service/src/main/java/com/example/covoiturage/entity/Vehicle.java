package com.example.covoiturage.entity;
//permet d'importer les annotations et les classes de JPA (Java Persistence API) pour faire le lien entre les classes Java et la base de données.
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vehicles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String brand;
    //définit les propriétés d'une colonne.
    @Column(nullable = false, length = 50)
    private String model;

    @Column(length = 30)
    private String color;

    private int seats;
    //les informations de User ne sont chargées que lorsqu'on en a besoin
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;
}

package com.example.covoiturage.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
//Permet de créer un objet en donnant les valeurs attribut par attribut.
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(length = 20)
    private String phone;
    //permet de stocker la valeur d'un enum sous forme de texte dans la base de données au lieu de stocker son numéro (index).
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;
    //permet de conserver la valeur par défaut même lors de la création d'un objet avec @Builder
    @Builder.Default
    private boolean blocked = false;
    //représente la note moyenne d'un utilisateur, calculée à partir des évaluations reçues.
    @Builder.Default
    private double averageRating = 0.0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    //déclenche automatiquement la méthode avant l'enregistrement dans la base de données.
    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}

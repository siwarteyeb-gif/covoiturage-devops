package com.example.covoiturage.config;

import com.example.covoiturage.entity.Role;
import com.example.covoiturage.entity.User;
import com.example.covoiturage.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Crée le compte administrateur au premier démarrage. */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (userRepository.existsByEmail(adminEmail)) {
            return;
        }
        User admin = User.builder()
                .name("Administrateur")
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .phone("00000000")
                .role(Role.ADMIN)
                .build();
        userRepository.save(admin);
        log.info("Compte administrateur créé : {}", adminEmail);
    }
}

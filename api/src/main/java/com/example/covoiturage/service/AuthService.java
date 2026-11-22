package com.example.covoiturage.service;

import com.example.covoiturage.dto.AuthResponse;
import com.example.covoiturage.dto.LoginRequest;
import com.example.covoiturage.dto.RegisterRequest;
import com.example.covoiturage.dto.UserResponse;
import com.example.covoiturage.entity.Role;
import com.example.covoiturage.entity.User;
import com.example.covoiturage.exception.ConflictException;
import com.example.covoiturage.exception.ResourceNotFoundException;
import com.example.covoiturage.repository.UserRepository;
import com.example.covoiturage.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    /** F01 : inscription. L'utilisateur est connecté directement après l'inscription. */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalize(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Cet email est déjà utilisé");
        }

        User user = User.builder()
                .name(request.name().trim())
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .phone(request.phone())
                .role(Role.USER)
                .build();
        User saved = userRepository.save(user);

        return toAuthResponse(saved);
    }

    /** F02 : connexion avec génération du token JWT (RG9 : compte bloqué refusé). */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = normalize(request.email());

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password()));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        return toAuthResponse(user);
    }

    private AuthResponse toAuthResponse(User user) {
        String token = jwtService.generateToken(user.getEmail(), user.getRole());
        return new AuthResponse(token, "Bearer", jwtService.getExpirationMs(), UserResponse.from(user));
    }

    private String normalize(String email) {
        return email.trim().toLowerCase();
    }
}

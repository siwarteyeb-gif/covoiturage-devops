package com.example.covoiturage.service;

import com.example.covoiturage.dto.ChangePasswordRequest;
import com.example.covoiturage.dto.UpdateProfileRequest;
import com.example.covoiturage.dto.UserResponse;
import com.example.covoiturage.entity.User;
import com.example.covoiturage.exception.BadRequestException;
import com.example.covoiturage.exception.ResourceNotFoundException;
import com.example.covoiturage.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;//Transaction de database

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /** F03 : consulter le profil. */
    @Transactional(readOnly = true)
    public UserResponse getProfile(String email) {
        return UserResponse.from(findByEmail(email));
    }

    /** F03 : modifier le profil (nom, téléphone). */
    @Transactional
    public UserResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = findByEmail(email);
        user.setName(request.name().trim());
        user.setPhone(request.phone());
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = findByEmail(email);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BadRequestException("Le mot de passe actuel est incorrect");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    private User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
    }
}

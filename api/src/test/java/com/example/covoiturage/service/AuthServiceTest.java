package com.example.covoiturage.service;

import com.example.covoiturage.dto.AuthResponse;
import com.example.covoiturage.dto.RegisterRequest;
import com.example.covoiturage.entity.Role;
import com.example.covoiturage.entity.User;
import com.example.covoiturage.exception.ConflictException;
import com.example.covoiturage.repository.UserRepository;
import com.example.covoiturage.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerCreatesUserWithEncodedPasswordAndUserRole() {
        RegisterRequest request = new RegisterRequest("Karim", "  Karim@Test.tn ", "22123456", "Password1");
        when(userRepository.existsByEmail("karim@test.tn")).thenReturn(false);
        when(passwordEncoder.encode("Password1")).thenReturn("HASH");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(jwtService.generateToken(anyString(), any(Role.class))).thenReturn("token-123");

        AuthResponse response = authService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertEquals("karim@test.tn", saved.getEmail());
        assertEquals("HASH", saved.getPassword());
        assertEquals(Role.USER, saved.getRole());
        assertFalse(saved.isBlocked());
        assertEquals("token-123", response.token());
        assertEquals("Bearer", response.tokenType());
    }

    @Test
    void registerWithExistingEmailThrowsConflict() {
        RegisterRequest request = new RegisterRequest("Karim", "karim@test.tn", "22123456", "Password1");
        when(userRepository.existsByEmail("karim@test.tn")).thenReturn(true);

        assertThrows(ConflictException.class, () -> authService.register(request));

        verify(userRepository, never()).save(any(User.class));
    }
}

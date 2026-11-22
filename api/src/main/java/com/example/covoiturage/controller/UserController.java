package com.example.covoiturage.controller;

import com.example.covoiturage.dto.ChangePasswordRequest;
import com.example.covoiturage.dto.UpdateProfileRequest;
import com.example.covoiturage.dto.UserResponse;
import com.example.covoiturage.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        return userService.getProfile(authentication.getName());
    }

    @PutMapping("/me")
    public UserResponse updateMe(Authentication authentication,
                                 @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(authentication.getName(), request);
    }

    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(Authentication authentication,
                               @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(authentication.getName(), request);
    }
}

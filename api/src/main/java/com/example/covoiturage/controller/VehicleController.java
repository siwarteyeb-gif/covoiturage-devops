package com.example.covoiturage.controller;

import com.example.covoiturage.dto.VehicleRequest;
import com.example.covoiturage.dto.VehicleResponse;
import com.example.covoiturage.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VehicleResponse create(Authentication authentication,
                                  @Valid @RequestBody VehicleRequest request) {
        return vehicleService.create(authentication.getName(), request);
    }

    @GetMapping("/me")
    public List<VehicleResponse> mine(Authentication authentication) {
        return vehicleService.listMine(authentication.getName());
    }

    @PutMapping("/{id}")
    public VehicleResponse update(Authentication authentication,
                                  @PathVariable Long id,
                                  @Valid @RequestBody VehicleRequest request) {
        return vehicleService.update(authentication.getName(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable Long id) {
        vehicleService.delete(authentication.getName(), id);
    }
}

package com.example.covoiturage.service;

import com.example.covoiturage.dto.VehicleRequest;
import com.example.covoiturage.dto.VehicleResponse;
import com.example.covoiturage.entity.User;
import com.example.covoiturage.entity.Vehicle;
import com.example.covoiturage.exception.ResourceNotFoundException;
import com.example.covoiturage.repository.UserRepository;
import com.example.covoiturage.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;

    /** F06 : ajouter un véhicule. */
    @Transactional
    public VehicleResponse create(String email, VehicleRequest request) {
        User owner = findUser(email);
        Vehicle vehicle = Vehicle.builder()
                .brand(request.brand().trim())
                .model(request.model().trim())
                .color(request.color() == null ? null : request.color().trim())
                .seats(request.seats())
                .owner(owner)
                .build();
        return VehicleResponse.from(vehicleRepository.save(vehicle));
    }

    @Transactional(readOnly = true)
    public List<VehicleResponse> listMine(String email) {
        User owner = findUser(email);
        return vehicleRepository.findByOwnerIdOrderByIdDesc(owner.getId())
                .stream()
                .map(VehicleResponse::from)
                .toList();
    }

    /** F07 : modifier un véhicule (uniquement le sien). */
    @Transactional
    public VehicleResponse update(String email, Long vehicleId, VehicleRequest request) {
        Vehicle vehicle = findOwnedVehicle(email, vehicleId);
        vehicle.setBrand(request.brand().trim());
        vehicle.setModel(request.model().trim());
        vehicle.setColor(request.color() == null ? null : request.color().trim());
        vehicle.setSeats(request.seats());
        return VehicleResponse.from(vehicleRepository.save(vehicle));
    }

    /** F07 : supprimer un véhicule (uniquement le sien). */
    @Transactional
    public void delete(String email, Long vehicleId) {
        vehicleRepository.delete(findOwnedVehicle(email, vehicleId));
    }

    // Un véhicule qui n'appartient pas à l'utilisateur est traité comme introuvable (404).
    private Vehicle findOwnedVehicle(String email, Long vehicleId) {
        User owner = findUser(email);
        return vehicleRepository.findByIdAndOwnerId(vehicleId, owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Véhicule introuvable"));
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
    }
}

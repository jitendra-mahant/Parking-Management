package com.parking.parking_backend.service;

import com.parking.parking_backend.dto.VehicleRequest;
import com.parking.parking_backend.dto.VehicleResponse;
import com.parking.parking_backend.model.User;
import com.parking.parking_backend.model.Vehicle;
import com.parking.parking_backend.repository.UserRepository;
import com.parking.parking_backend.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class VehicleService {

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private UserRepository userRepository;

    public List<VehicleResponse> getMyVehicles(String email) {
        return vehicleRepository.findByOwnerEmail(email).stream()
                .map(v -> new VehicleResponse(v.getId(), v.getVehicleNumber(), v.getVehicleType(), v.getModel()))
                .collect(Collectors.toList());
    }

    public VehicleResponse addVehicle(String email, VehicleRequest request) {
        if (vehicleRepository.existsByVehicleNumber(request.getVehicleNumber())) {
            throw new RuntimeException("This vehicle number is already registered");
        }

        User owner = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleNumber(request.getVehicleNumber().toUpperCase());
        vehicle.setVehicleType(request.getVehicleType());
        vehicle.setModel(request.getModel());
        vehicle.setOwner(owner);

        vehicleRepository.save(vehicle);

        return new VehicleResponse(vehicle.getId(), vehicle.getVehicleNumber(), vehicle.getVehicleType(), vehicle.getModel());
    }

    public void deleteVehicle(String email, Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        if (!vehicle.getOwner().getEmail().equals(email)) {
            throw new RuntimeException("You are not authorized to delete this vehicle");
        }

        vehicleRepository.delete(vehicle);
    }
}
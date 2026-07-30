package com.parking.parking_backend.repository;

import com.parking.parking_backend.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findByOwnerEmail(String email);
    boolean existsByVehicleNumber(String vehicleNumber);
}
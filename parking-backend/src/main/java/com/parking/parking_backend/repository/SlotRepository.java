package com.parking.parking_backend.repository;

import com.parking.parking_backend.model.Slot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SlotRepository extends JpaRepository<Slot, Long> {
    List<Slot> findByVehicleTypeAndOccupiedFalse(String vehicleType);
}
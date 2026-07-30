package com.parking.parking_backend.controller;

import com.parking.parking_backend.dto.VehicleRequest;
import com.parking.parking_backend.service.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    @Autowired
    private VehicleService vehicleService;

    @GetMapping
    public ResponseEntity<?> getMyVehicles(Authentication authentication) {
        try {
            return ResponseEntity.ok(vehicleService.getMyVehicles(authentication.getName()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> addVehicle(Authentication authentication, @RequestBody VehicleRequest request) {
        try {
            return ResponseEntity.ok(vehicleService.addVehicle(authentication.getName(), request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteVehicle(Authentication authentication, @PathVariable Long id) {
        try {
            vehicleService.deleteVehicle(authentication.getName(), id);
            return ResponseEntity.ok("Vehicle deleted successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
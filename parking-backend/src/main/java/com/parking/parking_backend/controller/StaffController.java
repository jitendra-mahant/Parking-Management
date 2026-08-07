package com.parking.parking_backend.controller;

import com.parking.parking_backend.dto.VehicleEntryRequest;
import com.parking.parking_backend.service.StaffService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/staff")
public class StaffController {

    @Autowired
    private StaffService staffService;

    @PostMapping("/entry")
    public ResponseEntity<?> recordEntry(Authentication authentication, @RequestBody VehicleEntryRequest request) {
        try {
            return ResponseEntity.ok(staffService.recordEntry(authentication.getName(), request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/exit/{bookingId}")
    public ResponseEntity<?> recordExit(Authentication authentication, @PathVariable Long bookingId) {
        try {
            return ResponseEntity.ok(staffService.recordExit(authentication.getName(), bookingId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/active-bookings")
    public ResponseEntity<?> getAllActiveBookings() {
        try {
            return ResponseEntity.ok(staffService.getAllActiveBookings());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/slots")
    public ResponseEntity<?> getAllSlots() {
        try {
            return ResponseEntity.ok(staffService.getAllSlots());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/quick-stats")
    public ResponseEntity<?> getQuickStats() {
        try {
            return ResponseEntity.ok(staffService.getQuickStats());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
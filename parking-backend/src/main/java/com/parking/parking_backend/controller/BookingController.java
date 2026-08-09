package com.parking.parking_backend.controller;

import com.parking.parking_backend.dto.BookSlotRequest;
import com.parking.parking_backend.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @GetMapping("/available-slots")
    public ResponseEntity<?> getAvailableSlots(@RequestParam String vehicleType) {
        try {
            return ResponseEntity.ok(bookingService.getAvailableSlots(vehicleType));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> bookSlot(Authentication authentication, @RequestBody BookSlotRequest request) {
        try {
            return ResponseEntity.ok(bookingService.bookSlot(authentication.getName(), request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getMyBookings(Authentication authentication) {
        try {
            return ResponseEntity.ok(bookingService.getMyBookings(authentication.getName()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelBooking(Authentication authentication, @PathVariable Long id) {
        try {
            return ResponseEntity.ok(bookingService.cancelBooking(authentication.getName(), id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<?> completeBooking(Authentication authentication, @PathVariable Long id) {
        try {
            return ResponseEntity.ok(bookingService.completeBooking(authentication.getName(), id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/my-stats")
    public ResponseEntity<?> getMyStats(Authentication authentication) {
        try {
            return ResponseEntity.ok(bookingService.getMyStats(authentication.getName()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
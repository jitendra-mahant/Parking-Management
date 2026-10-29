package com.parking.parking_backend.controller;

import com.parking.parking_backend.dto.BookSlotRequest;
import com.parking.parking_backend.dto.RateBookingRequest;
import com.parking.parking_backend.service.BookingService;
import com.parking.parking_backend.util.PricingConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @GetMapping("/available-slots")
    public ResponseEntity<?> getAvailableSlots(
            @RequestParam String vehicleType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime checkOut) {
        try {
            return ResponseEntity.ok(bookingService.getAvailableSlots(vehicleType, checkIn, checkOut));
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

    @PutMapping("/{id}/extend")
    public ResponseEntity<?> extendBooking(Authentication authentication, @PathVariable Long id) {
        try {
            return ResponseEntity.ok(bookingService.extendBooking(authentication.getName(), id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}/rate")
    public ResponseEntity<?> rateBooking(Authentication authentication, @PathVariable Long id,
                                          @RequestBody RateBookingRequest request) {
        try {
            return ResponseEntity.ok(bookingService.rateBooking(
                    authentication.getName(), id, request.getRating(), request.getFeedback()));
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

    @GetMapping("/rate")
    public ResponseEntity<?> getRate(@RequestParam String vehicleType) {
        return ResponseEntity.ok(Map.of(
                "vehicleType", vehicleType,
                "ratePerHour", PricingConfig.getRateForType(vehicleType)));
    }
}
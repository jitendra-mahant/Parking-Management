package com.parking.parking_backend.controller;

import com.parking.parking_backend.dto.CreateStaffRequest;
import com.parking.parking_backend.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @PostMapping("/staff")
    public ResponseEntity<?> createStaff(@RequestBody CreateStaffRequest request) {
        try {
            return ResponseEntity.ok(adminService.createStaff(request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/staff")
    public ResponseEntity<?> getAllStaff() {
        try {
            return ResponseEntity.ok(adminService.getAllStaff());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/staff/{id}")
    public ResponseEntity<?> deleteStaff(@PathVariable Long id) {
        try {
            adminService.deleteStaff(id);
            return ResponseEntity.ok("Staff removed successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/bookings")
    public ResponseEntity<?> getAllBookings() {
        try {
            return ResponseEntity.ok(adminService.getAllBookings());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/revenue")
    public ResponseEntity<?> getRevenueReport() {
        try {
            return ResponseEntity.ok(adminService.getRevenueReport());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/revenue-chart")
    public ResponseEntity<?> getRevenueChart() {
        try {
            return ResponseEntity.ok(adminService.getRevenueChart());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}

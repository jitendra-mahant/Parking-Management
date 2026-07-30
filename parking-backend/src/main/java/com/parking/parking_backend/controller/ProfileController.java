package com.parking.parking_backend.controller;

import com.parking.parking_backend.dto.ChangePasswordRequest;
import com.parking.parking_backend.dto.UpdateProfileRequest;
import com.parking.parking_backend.service.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    @Autowired
    private ProfileService profileService;

    @GetMapping
    public ResponseEntity<?> getProfile(Authentication authentication) {
        try {
            String email = authentication.getName();
            return ResponseEntity.ok(profileService.getProfile(email));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping
    public ResponseEntity<?> updateProfile(Authentication authentication, @RequestBody UpdateProfileRequest request) {
        try {
            String email = authentication.getName();
            return ResponseEntity.ok(profileService.updateProfile(email, request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(Authentication authentication, @RequestBody ChangePasswordRequest request) {
        try {
            String email = authentication.getName();
            profileService.changePassword(email, request);
            return ResponseEntity.ok("Password changed successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
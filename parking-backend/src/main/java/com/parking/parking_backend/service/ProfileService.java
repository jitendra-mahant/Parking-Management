package com.parking.parking_backend.service;

import com.parking.parking_backend.dto.ChangePasswordRequest;
import com.parking.parking_backend.dto.ProfileResponse;
import com.parking.parking_backend.dto.UpdateProfileRequest;
import com.parking.parking_backend.model.User;
import com.parking.parking_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public ProfileResponse getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return new ProfileResponse(user.getName(), user.getEmail(), user.getPhone(), user.getRole());
    }

    public ProfileResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getPhone() != null && !request.getPhone().isEmpty()
                && !request.getPhone().matches("\\d{10}")) {
            throw new RuntimeException("Phone number must be exactly 10 digits");
        }

        if (request.getName() != null && !request.getName().isEmpty()) {
            user.setName(request.getName());
        }
        if (request.getPhone() != null && !request.getPhone().isEmpty()) {
            user.setPhone(request.getPhone());
        }

        userRepository.save(user);

        return new ProfileResponse(user.getName(), user.getEmail(), user.getPhone(), user.getRole());
    }

    public void changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}
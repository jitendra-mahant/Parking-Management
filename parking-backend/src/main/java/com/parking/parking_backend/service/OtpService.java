package com.parking.parking_backend.service;

import com.parking.parking_backend.model.PasswordResetOtp;
import com.parking.parking_backend.model.User;
import com.parking.parking_backend.repository.PasswordResetOtpRepository;
import com.parking.parking_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class OtpService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetOtpRepository otpRepository;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Step 1: Generate OTP and email it
    public void sendOtp(String email) {
        if (!userRepository.existsByEmail(email)) {
            throw new RuntimeException("No account found with this email");
        }

        String otp = String.valueOf(100000 + new Random().nextInt(900000)); // 6-digit OTP

        PasswordResetOtp otpEntity = new PasswordResetOtp();
        otpEntity.setEmail(email);
        otpEntity.setOtp(otp);
        otpEntity.setExpiryTime(LocalDateTime.now().plusMinutes(10));
        otpEntity.setVerified(false);

        otpRepository.save(otpEntity);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Parking Management - Password Reset OTP");
        message.setText("Your OTP for password reset is: " + otp + "\nThis OTP expires in 10 minutes.");

        mailSender.send(message);
    }

    // Step 2: Verify OTP
    public void verifyOtp(String email, String otp) {
        PasswordResetOtp record = otpRepository.findTopByEmailOrderByIdDesc(email)
                .orElseThrow(() -> new RuntimeException("No OTP request found for this email"));

        if (record.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP has expired. Please request a new one");
        }

        if (!record.getOtp().equals(otp)) {
            throw new RuntimeException("Invalid OTP");
        }

        record.setVerified(true);
        otpRepository.save(record);
    }

    // Step 3: Reset Password (only allowed if OTP was verified)
    public void resetPassword(String email, String newPassword) {
        PasswordResetOtp record = otpRepository.findTopByEmailOrderByIdDesc(email)
                .orElseThrow(() -> new RuntimeException("No OTP request found for this email"));

        if (!record.isVerified()) {
            throw new RuntimeException("OTP not verified yet");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
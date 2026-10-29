package com.parking.parking_backend.service;

import com.parking.parking_backend.dto.*;
import com.parking.parking_backend.model.Booking;
import com.parking.parking_backend.model.User;
import com.parking.parking_backend.repository.BookingRepository;
import com.parking.parking_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public StaffResponse createStaff(CreateStaffRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        if (request.getPhone() != null && !request.getPhone().isEmpty()
                && !request.getPhone().matches("\\d{10}")) {
            throw new RuntimeException("Phone number must be exactly 10 digits");
        }

        User staff = new User();
        staff.setName(request.getName());
        staff.setEmail(request.getEmail());
        staff.setPassword(passwordEncoder.encode(request.getPassword()));
        staff.setPhone(request.getPhone());
        staff.setRole("staff");

        userRepository.save(staff);

        return new StaffResponse(staff.getId(), staff.getName(), staff.getEmail(), staff.getPhone());
    }

    public List<StaffResponse> getAllStaff() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole().equals("staff"))
                .map(u -> new StaffResponse(u.getId(), u.getName(), u.getEmail(), u.getPhone()))
                .collect(Collectors.toList());
    }

    public void deleteStaff(Long staffId) {
        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new RuntimeException("Staff not found"));

        if (!staff.getRole().equals("staff")) {
            throw new RuntimeException("This user is not a staff member");
        }

        userRepository.delete(staff);
    }

    public List<BookingResponse> getAllBookings() {
        return bookingRepository.findAll().stream()
                .map(b -> {
                    BookingResponse response = new BookingResponse(
                            b.getId(),
                            b.getSlot().getSlotNumber(),
                            b.getVehicle().getVehicleNumber(),
                            b.getEntryTime(),
                            b.getExitTime(),
                            b.getStatus(),
                            b.getFare(),
                            b.getExpectedExitTime()
                    );
                    response.setRating(b.getRating());
                    response.setFeedback(b.getFeedback());
                    return response;
                })
                .collect(Collectors.toList());
    }

    public RevenueReport getRevenueReport() {
        List<Booking> allBookings = bookingRepository.findAll();

        long total = allBookings.size();
        long active = allBookings.stream().filter(b -> b.getStatus().equals("ACTIVE")).count();
        long completed = allBookings.stream().filter(b -> b.getStatus().equals("COMPLETED")).count();
        double revenue = allBookings.stream()
                .filter(b -> b.getFare() != null)
                .mapToDouble(Booking::getFare)
                .sum();

        return new RevenueReport(total, active, completed, revenue);
    }

    public List<Map<String, Object>> getRevenueChart() {
        List<Booking> allBookings = bookingRepository.findAll();
        Map<String, Double> dailyMap = new TreeMap<>();

        LocalDate today = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (int i = 6; i >= 0; i--) {
            dailyMap.put(today.minusDays(i).format(fmt), 0.0);
        }

        for (Booking b : allBookings) {
            if ("COMPLETED".equals(b.getStatus()) && b.getFare() != null) {
                LocalDateTime time = b.getExitTime() != null ? b.getExitTime() : b.getEntryTime();
                if (time != null) {
                    String dateKey = time.toLocalDate().format(fmt);
                    dailyMap.put(dateKey, dailyMap.getOrDefault(dateKey, 0.0) + b.getFare());
                }
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Double> entry : dailyMap.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("date", entry.getKey());
            item.put("revenue", entry.getValue());
            result.add(item);
        }
        return result;
    }
}

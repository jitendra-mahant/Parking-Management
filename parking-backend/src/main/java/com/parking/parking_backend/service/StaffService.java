package com.parking.parking_backend.service;

import com.parking.parking_backend.dto.BookingResponse;
import com.parking.parking_backend.dto.VehicleEntryRequest;
import com.parking.parking_backend.model.*;
import com.parking.parking_backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StaffService {

    @Autowired
    private SlotRepository slotRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    private static final double RATE_PER_HOUR = 20.0;

    // Staff records a walk-in vehicle entry
    public BookingResponse recordEntry(String staffEmail, VehicleEntryRequest request) {
        User staff = userRepository.findByEmail(staffEmail)
                .orElseThrow(() -> new RuntimeException("Staff not found"));

        List<Slot> availableSlots = slotRepository.findByVehicleTypeAndOccupiedFalse(request.getVehicleType());
        if (availableSlots.isEmpty()) {
            throw new RuntimeException("No available slots for " + request.getVehicleType());
        }

        String vehicleNumber = request.getVehicleNumber().toUpperCase();

        // Find existing vehicle by number, or create a walk-in vehicle record owned by staff
        Vehicle vehicle = vehicleRepository.findAll().stream()
                .filter(v -> v.getVehicleNumber().equals(vehicleNumber))
                .findFirst()
                .orElseGet(() -> {
                    Vehicle newVehicle = new Vehicle();
                    newVehicle.setVehicleNumber(vehicleNumber);
                    newVehicle.setVehicleType(request.getVehicleType());
                    newVehicle.setModel("Walk-in");
                    newVehicle.setOwner(staff); // walk-in vehicles attributed to staff who registered them
                    return vehicleRepository.save(newVehicle);
                });

        Slot slot = availableSlots.get(0);
        slot.setOccupied(true);
        slotRepository.save(slot);

        Booking booking = new Booking();
        booking.setCustomer(staff); // staff-managed booking
        booking.setVehicle(vehicle);
        booking.setSlot(slot);
        booking.setEntryTime(LocalDateTime.now());
        booking.setStatus("ACTIVE");

        bookingRepository.save(booking);

        return toResponse(booking);
    }

    // Staff processes vehicle exit + collects payment
    public BookingResponse recordExit(String staffEmail, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!booking.getStatus().equals("ACTIVE")) {
            throw new RuntimeException("This booking is not active");
        }

        LocalDateTime exitTime = LocalDateTime.now();
        long minutes = Duration.between(booking.getEntryTime(), exitTime).toMinutes();
        double hours = Math.max(1, Math.ceil(minutes / 60.0));
        double fare = hours * RATE_PER_HOUR;

        booking.setExitTime(exitTime);
        booking.setStatus("COMPLETED");
        booking.setFare(fare);

        Slot slot = booking.getSlot();
        slot.setOccupied(false);
        slotRepository.save(slot);

        bookingRepository.save(booking);

        return toResponse(booking);
    }

    // View all currently active bookings (across all customers)
    public List<BookingResponse> getAllActiveBookings() {
        return bookingRepository.findAll().stream()
                .filter(b -> b.getStatus().equals("ACTIVE"))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // View all slots with their status
    public List<Slot> getAllSlots() {
        return slotRepository.findAll();
    }

    private BookingResponse toResponse(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getSlot().getSlotNumber(),
                booking.getVehicle().getVehicleNumber(),
                booking.getEntryTime(),
                booking.getExitTime(),
                booking.getStatus(),
                booking.getFare()
        );
    }

    public java.util.Map<String, Object> getQuickStats() {
        List<Slot> allSlots = slotRepository.findAll();
        long totalSlots = allSlots.size();
        long occupiedSlots = allSlots.stream().filter(Slot::isOccupied).count();
        long availableSlots = totalSlots - occupiedSlots;

        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        long todayEntries = bookingRepository.findAll().stream()
                .filter(b -> b.getEntryTime().isAfter(todayStart))
                .count();
        long todayExits = bookingRepository.findAll().stream()
                .filter(b -> b.getExitTime() != null && b.getExitTime().isAfter(todayStart))
                .count();

        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalSlots", totalSlots);
        stats.put("occupiedSlots", occupiedSlots);
        stats.put("availableSlots", availableSlots);
        stats.put("todayEntries", todayEntries);
        stats.put("todayExits", todayExits);
        return stats;
    }
}
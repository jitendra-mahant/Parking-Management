package com.parking.parking_backend.service;

import com.parking.parking_backend.dto.BookSlotRequest;
import com.parking.parking_backend.dto.BookingResponse;
import com.parking.parking_backend.model.*;
import com.parking.parking_backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.parking.parking_backend.util.PricingConfig;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookingService {

    @Autowired
    private SlotRepository slotRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private NotificationService notificationService;

    public List<Slot> getAvailableSlots(String vehicleType) {
        return slotRepository.findByVehicleTypeAndOccupiedFalse(vehicleType);
    }

    public BookingResponse bookSlot(String email, BookSlotRequest request) {
        User customer = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        if (!vehicle.getOwner().getEmail().equals(email)) {
            throw new RuntimeException("This vehicle does not belong to you");
        }

        List<Slot> availableSlots = slotRepository.findByVehicleTypeAndOccupiedFalse(request.getVehicleType());

        if (availableSlots.isEmpty()) {
            throw new RuntimeException("No available slots for " + request.getVehicleType());
        }

        Slot slot = availableSlots.get(0); // pick the first available slot
        slot.setOccupied(true);
        slotRepository.save(slot);

        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setVehicle(vehicle);
        booking.setSlot(slot);
        booking.setEntryTime(LocalDateTime.now());
        booking.setStatus("ACTIVE");

        bookingRepository.save(booking);

        notificationService.createNotification(customer,
                "Your booking for slot " + slot.getSlotNumber() + " is confirmed.");

        return toResponse(booking);
    }

    public List<BookingResponse> getMyBookings(String email) {
        return bookingRepository.findByCustomerEmailOrderByEntryTimeDesc(email).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public BookingResponse cancelBooking(String email, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!booking.getCustomer().getEmail().equals(email)) {
            throw new RuntimeException("You are not authorized to cancel this booking");
        }

        if (!booking.getStatus().equals("ACTIVE")) {
            throw new RuntimeException("Only active bookings can be cancelled");
        }

        booking.setStatus("CANCELLED");
        booking.setExitTime(LocalDateTime.now());

        Slot slot = booking.getSlot();
        slot.setOccupied(false);
        slotRepository.save(slot);

        bookingRepository.save(booking);

        notificationService.createNotification(booking.getCustomer(),
                "Your booking for slot " + booking.getSlot().getSlotNumber() + " was cancelled.");

        return toResponse(booking);
    }

    public BookingResponse completeBooking(String email, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!booking.getCustomer().getEmail().equals(email)) {
            throw new RuntimeException("You are not authorized to complete this booking");
        }

        if (!booking.getStatus().equals("ACTIVE")) {
            throw new RuntimeException("Only active bookings can be completed");
        }

                LocalDateTime exitTime = LocalDateTime.now();
        long minutes = Duration.between(booking.getEntryTime(), exitTime).toMinutes();
        double hours = Math.max(1, Math.ceil(minutes / 60.0)); // minimum 1 hour charge
        double rate = PricingConfig.getRateForType(booking.getVehicle().getVehicleType());
        double fare = hours * rate;

        booking.setExitTime(exitTime);
        booking.setStatus("COMPLETED");
        booking.setFare(fare);

        Slot slot = booking.getSlot();
        slot.setOccupied(false);
        slotRepository.save(slot);

        bookingRepository.save(booking);

        notificationService.createNotification(booking.getCustomer(),
                "Your booking for slot " + booking.getSlot().getSlotNumber() + " is complete. Fare: ₹" + fare);

        return toResponse(booking);
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

    public java.util.Map<String, Object> getMyStats(String email) {
        List<Booking> myBookings = bookingRepository.findByCustomerEmailOrderByEntryTimeDesc(email);

        long totalBookings = myBookings.size();
        long activeBookings = myBookings.stream().filter(b -> b.getStatus().equals("ACTIVE")).count();
        double totalSpent = myBookings.stream()
                .filter(b -> b.getFare() != null)
                .mapToDouble(Booking::getFare)
                .sum();

        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalBookings", totalBookings);
        stats.put("activeBookings", activeBookings);
        stats.put("totalSpent", totalSpent);
        return stats;
    }
}
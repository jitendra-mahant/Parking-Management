package com.parking.parking_backend.service;

import com.parking.parking_backend.dto.BookSlotRequest;
import com.parking.parking_backend.dto.BookingResponse;
import com.parking.parking_backend.model.*;
import com.parking.parking_backend.repository.*;
import com.parking.parking_backend.util.BookingMapper;
import com.parking.parking_backend.util.FareCalculator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    @Autowired
    private SlotAvailabilityService availabilityService;

    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    public List<Slot> getAvailableSlots(String vehicleType, LocalDateTime checkIn, LocalDateTime checkOut) {
        validateWindow(checkIn, checkOut);
        return availabilityService.findFreeSlots(vehicleType, checkIn, checkOut);
    }

    // synchronized so two simultaneous requests cannot take the same slot
    public synchronized BookingResponse bookSlot(String email, BookSlotRequest request) {
        User customer = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        if (!vehicle.getOwner().getEmail().equals(email)) {
            throw new RuntimeException("This vehicle does not belong to you");
        }

        LocalDateTime checkIn = request.getCheckIn();
        LocalDateTime checkOut = request.getCheckOut();
        validateWindow(checkIn, checkOut);

        if (!availabilityService.isVehicleFree(vehicle.getId(), checkIn, checkOut, null)) {
            throw new RuntimeException("This vehicle already has a booking during the selected time");
        }

        Slot slot;
        if (request.getSlotId() != null) {
            slot = slotRepository.findById(request.getSlotId())
                    .orElseThrow(() -> new RuntimeException("Slot not found"));

            if (!slot.getVehicleType().equals(vehicle.getVehicleType())) {
                throw new RuntimeException("Slot " + slot.getSlotNumber() + " is not for " + vehicle.getVehicleType() + " vehicles");
            }
            if (!availabilityService.isSlotFree(slot.getId(), checkIn, checkOut, null)) {
                throw new RuntimeException("Slot " + slot.getSlotNumber() + " is already booked for the selected time. Please choose another slot.");
            }
        } else {
            List<Slot> free = availabilityService.findFreeSlots(vehicle.getVehicleType(), checkIn, checkOut);
            if (free.isEmpty()) {
                throw new RuntimeException("No available slots for " + vehicle.getVehicleType() + " in the selected time");
            }
            slot = free.get(0);
        }

        double estimated = FareCalculator.estimate(vehicle.getVehicleType(), checkIn, checkOut);

        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setVehicle(vehicle);
        booking.setSlot(slot);
        booking.setEntryTime(checkIn);
        booking.setExpectedExitTime(checkOut);
        booking.setEstimatedFare(estimated);
        booking.setStatus("ACTIVE");
        bookingRepository.save(booking);

        notificationService.createNotification(customer,
                "Your booking for slot " + slot.getSlotNumber() + " is confirmed from "
                        + checkIn.format(DISPLAY) + " to " + checkOut.format(DISPLAY)
                        + ". Estimated fare: ₹" + String.format("%.2f", estimated));

        return BookingMapper.toResponse(booking);
    }

    public List<BookingResponse> getMyBookings(String email) {
        return bookingRepository.findByCustomerEmailOrderByEntryTimeDesc(email).stream()
                .map(BookingMapper::toResponse)
                .collect(Collectors.toList());
    }

    public BookingResponse cancelBooking(String email, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!booking.getCustomer().getEmail().equals(email)) {
            throw new RuntimeException("You are not authorized to cancel this booking");
        }

        if (!"ACTIVE".equals(booking.getStatus())) {
            throw new RuntimeException("Only active bookings can be cancelled");
        }

        booking.setStatus("CANCELLED");
        booking.setExitTime(LocalDateTime.now());
        bookingRepository.save(booking);

        notificationService.createNotification(booking.getCustomer(),
                "Your booking for slot " + booking.getSlot().getSlotNumber() + " was cancelled.");

        return BookingMapper.toResponse(booking);
    }

    public BookingResponse completeBooking(String email, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!booking.getCustomer().getEmail().equals(email)) {
            throw new RuntimeException("You are not authorized to complete this booking");
        }

        if (!"ACTIVE".equals(booking.getStatus())) {
            throw new RuntimeException("Only active bookings can be completed");
        }

        LocalDateTime now = LocalDateTime.now();
        if (booking.getEntryTime().isAfter(now)) {
            throw new RuntimeException("Check-in time (" + booking.getEntryTime().format(DISPLAY)
                    + ") has not started yet. You can cancel this booking instead.");
        }

        double fare = FareCalculator.finalFare(booking, now);

        booking.setExitTime(now);
        booking.setStatus("COMPLETED");
        booking.setFare(fare);
        bookingRepository.save(booking);

        notificationService.createNotification(booking.getCustomer(),
                "Your booking for slot " + booking.getSlot().getSlotNumber() + " is complete. Fare: ₹" + fare);

        return BookingMapper.toResponse(booking);
    }

    public synchronized BookingResponse extendBooking(String email, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!booking.getCustomer().getEmail().equals(email)) {
            throw new RuntimeException("You are not authorized to extend this booking");
        }

        if (!"ACTIVE".equals(booking.getStatus())) {
            throw new RuntimeException("Only active bookings can be extended");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime currentEnd = booking.getExpectedExitTime() != null
                ? booking.getExpectedExitTime() : booking.getEntryTime().plusHours(2);
        LocalDateTime base = currentEnd.isBefore(now) ? now : currentEnd;
        LocalDateTime newEnd = base.plusHours(1);

        if (!availabilityService.isSlotFree(booking.getSlot().getId(), base, newEnd, booking.getId())) {
            throw new RuntimeException("Cannot extend: this slot is reserved by another booking right after your check-out");
        }

        booking.setExpectedExitTime(newEnd);
        if (booking.getEstimatedFare() != null) {
            booking.setEstimatedFare(FareCalculator.estimate(
                    booking.getVehicle().getVehicleType(), booking.getEntryTime(), newEnd));
        }
        bookingRepository.save(booking);

        return BookingMapper.toResponse(booking);
    }

    public BookingResponse rateBooking(String email, Long bookingId, Integer rating, String feedback) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!booking.getCustomer().getEmail().equals(email)) {
            throw new RuntimeException("You are not authorized to rate this booking");
        }

        if (!"COMPLETED".equals(booking.getStatus())) {
            throw new RuntimeException("Only completed bookings can be rated");
        }

        if (rating == null || rating < 1 || rating > 5) {
            throw new RuntimeException("Rating must be between 1 and 5");
        }

        booking.setRating(rating);
        booking.setFeedback(feedback);
        bookingRepository.save(booking);

        return BookingMapper.toResponse(booking);
    }

    public Map<String, Object> getMyStats(String email) {
        List<Booking> myBookings = bookingRepository.findByCustomerEmailOrderByEntryTimeDesc(email);

        long totalBookings = myBookings.size();
        long activeBookings = myBookings.stream().filter(b -> "ACTIVE".equals(b.getStatus())).count();
        double totalSpent = myBookings.stream()
                .filter(b -> b.getFare() != null)
                .mapToDouble(Booking::getFare)
                .sum();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalBookings", totalBookings);
        stats.put("activeBookings", activeBookings);
        stats.put("totalSpent", totalSpent);
        return stats;
    }

    private void validateWindow(LocalDateTime checkIn, LocalDateTime checkOut) {
        if (checkIn == null || checkOut == null) {
            throw new RuntimeException("Check-in and check-out date and time are required");
        }

        LocalDateTime now = LocalDateTime.now();

        if (checkIn.isBefore(now.minusMinutes(5))) {
            throw new RuntimeException("Check-in time cannot be in the past");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new RuntimeException("Check-out must be after check-in");
        }

        long minutes = Duration.between(checkIn, checkOut).toMinutes();
        if (minutes < 60) {
            throw new RuntimeException("Minimum booking duration is 1 hour");
        }
        if (minutes > 7 * 24 * 60) {
            throw new RuntimeException("Maximum booking duration is 7 days");
        }
        if (checkIn.isAfter(now.plusDays(30))) {
            throw new RuntimeException("Bookings can be made at most 30 days in advance");
        }
    }
}
package com.parking.parking_backend.service;

import com.parking.parking_backend.dto.BookingResponse;
import com.parking.parking_backend.dto.VehicleEntryRequest;
import com.parking.parking_backend.model.*;
import com.parking.parking_backend.repository.*;
import com.parking.parking_backend.util.BookingMapper;
import com.parking.parking_backend.util.FareCalculator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    @Autowired
    private SlotAvailabilityService availabilityService;

    @Autowired
    private NotificationService notificationService;

    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    // Staff records a walk-in vehicle entry (check-in is now, expected check-out is +2 hours)
    public synchronized BookingResponse recordEntry(String staffEmail, VehicleEntryRequest request) {
        User staff = userRepository.findByEmail(staffEmail)
                .orElseThrow(() -> new RuntimeException("Staff not found"));

        String vehicleNumber = request.getVehicleNumber().toUpperCase();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime plannedExit = now.plusHours(2);

        Vehicle vehicle = vehicleRepository.findAll().stream()
                .filter(v -> v.getVehicleNumber().equals(vehicleNumber))
                .findFirst()
                .orElse(null);

        if (vehicle != null && !availabilityService.isVehicleFree(vehicle.getId(), now, plannedExit, null)) {
            throw new RuntimeException("This vehicle already has an active booking");
        }

        String type = vehicle != null ? vehicle.getVehicleType() : request.getVehicleType();

        List<Slot> free = availabilityService.findFreeSlots(type, now, plannedExit);
        if (free.isEmpty()) {
            throw new RuntimeException("No available slots for " + type + " right now");
        }

        if (vehicle == null) {
            Vehicle newVehicle = new Vehicle();
            newVehicle.setVehicleNumber(vehicleNumber);
            newVehicle.setVehicleType(request.getVehicleType());
            newVehicle.setModel("Walk-in");
            newVehicle.setOwner(staff);
            vehicle = vehicleRepository.save(newVehicle);
        }

        Booking booking = new Booking();
        booking.setCustomer(staff);
        booking.setVehicle(vehicle);
        booking.setSlot(free.get(0));
        booking.setEntryTime(now);
        booking.setExpectedExitTime(plannedExit);
        booking.setStatus("ACTIVE");
        bookingRepository.save(booking);

        return BookingMapper.toResponse(booking);
    }

    // Staff processes vehicle exit and fare
    public BookingResponse recordExit(String staffEmail, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!"ACTIVE".equals(booking.getStatus())) {
            throw new RuntimeException("This booking is not active");
        }

        LocalDateTime now = LocalDateTime.now();
        if (booking.getEntryTime().isAfter(now)) {
            throw new RuntimeException("This booking has not started yet (check-in is at "
                    + booking.getEntryTime().format(DISPLAY) + ")");
        }

        double fare = FareCalculator.finalFare(booking, now);

        booking.setExitTime(now);
        booking.setStatus("COMPLETED");
        booking.setFare(fare);
        bookingRepository.save(booking);

        if ("customer".equals(booking.getCustomer().getRole())) {
            notificationService.createNotification(booking.getCustomer(),
                    "Your booking for slot " + booking.getSlot().getSlotNumber() + " is complete. Fare: ₹" + fare);
        }

        return BookingMapper.toResponse(booking);
    }

    // All ACTIVE bookings (parked, overstayed and upcoming)
    public List<BookingResponse> getAllActiveBookings() {
        return bookingRepository.findByStatus("ACTIVE").stream()
                .map(BookingMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<Slot> getAllSlots() {
        syncSlotOccupancy();
        return slotRepository.findAll();
    }

    public Map<String, Object> getQuickStats() {
        syncSlotOccupancy();

        List<Slot> allSlots = slotRepository.findAll();
        long totalSlots = allSlots.size();
        long occupiedSlots = allSlots.stream().filter(Slot::isOccupied).count();
        long availableSlots = totalSlots - occupiedSlots;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
        List<Booking> all = bookingRepository.findAll();

        long todayEntries = all.stream()
                .filter(b -> !"CANCELLED".equals(b.getStatus()))
                .filter(b -> !b.getEntryTime().isBefore(todayStart) && !b.getEntryTime().isAfter(now))
                .count();

        long todayExits = all.stream()
                .filter(b -> "COMPLETED".equals(b.getStatus()))
                .filter(b -> b.getExitTime() != null && !b.getExitTime().isBefore(todayStart))
                .count();

        long overstayCount = all.stream()
                .filter(b -> "ACTIVE".equals(b.getStatus()))
                .filter(b -> !b.getEntryTime().isAfter(now))
                .filter(b -> b.getExpectedExitTime() != null && b.getExpectedExitTime().isBefore(now))
                .count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSlots", totalSlots);
        stats.put("occupiedSlots", occupiedSlots);
        stats.put("availableSlots", availableSlots);
        stats.put("todayEntries", todayEntries);
        stats.put("todayExits", todayExits);
        stats.put("overstayCount", overstayCount);
        return stats;
    }

    // A slot is physically occupied now if an ACTIVE booking has already checked in
    private void syncSlotOccupancy() {
        LocalDateTime now = LocalDateTime.now();

        Set<Long> occupiedIds = bookingRepository.findByStatus("ACTIVE").stream()
                .filter(b -> !b.getEntryTime().isAfter(now))
                .map(b -> b.getSlot().getId())
                .collect(Collectors.toSet());

        for (Slot slot : slotRepository.findAll()) {
            boolean occupied = occupiedIds.contains(slot.getId());
            if (slot.isOccupied() != occupied) {
                slot.setOccupied(occupied);
                slotRepository.save(slot);
            }
        }
    }
}
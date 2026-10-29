package com.parking.parking_backend.service;

import com.parking.parking_backend.model.Booking;
import com.parking.parking_backend.model.Slot;
import com.parking.parking_backend.repository.BookingRepository;
import com.parking.parking_backend.repository.SlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SlotAvailabilityService {

    @Autowired
    private SlotRepository slotRepository;

    @Autowired
    private BookingRepository bookingRepository;

    private static final LocalDateTime FAR_FUTURE = LocalDateTime.of(9999, 12, 31, 23, 59);

    // All slots of this vehicle type that are free for the whole window [from, to)
    public List<Slot> findFreeSlots(String vehicleType, LocalDateTime from, LocalDateTime to) {
        List<Booking> active = bookingRepository.findByStatus("ACTIVE");
        LocalDateTime now = LocalDateTime.now();

        return slotRepository.findByVehicleType(vehicleType).stream()
                .filter(slot -> active.stream()
                        .filter(b -> b.getSlot().getId().equals(slot.getId()))
                        .noneMatch(b -> overlaps(b, from, to, now)))
                .collect(Collectors.toList());
    }

    public boolean isSlotFree(Long slotId, LocalDateTime from, LocalDateTime to, Long ignoreBookingId) {
        LocalDateTime now = LocalDateTime.now();
        for (Booking b : bookingRepository.findByStatus("ACTIVE")) {
            if (!b.getSlot().getId().equals(slotId)) continue;
            if (ignoreBookingId != null && b.getId().equals(ignoreBookingId)) continue;
            if (overlaps(b, from, to, now)) return false;
        }
        return true;
    }

    // A vehicle cannot hold two overlapping bookings
    public boolean isVehicleFree(Long vehicleId, LocalDateTime from, LocalDateTime to, Long ignoreBookingId) {
        LocalDateTime now = LocalDateTime.now();
        for (Booking b : bookingRepository.findByStatus("ACTIVE")) {
            if (!b.getVehicle().getId().equals(vehicleId)) continue;
            if (ignoreBookingId != null && b.getId().equals(ignoreBookingId)) continue;
            if (overlaps(b, from, to, now)) return false;
        }
        return true;
    }

    private static boolean overlaps(Booking b, LocalDateTime from, LocalDateTime to, LocalDateTime now) {
        LocalDateTime start = b.getEntryTime();
        LocalDateTime end = b.getExpectedExitTime() != null ? b.getExpectedExitTime() : start.plusHours(2);

        // a vehicle that is still parked past its check-out keeps blocking the slot
        if (!start.isAfter(now) && end.isBefore(now)) {
            end = FAR_FUTURE;
        }
        return start.isBefore(to) && end.isAfter(from);
    }
}
package com.parking.parking_backend.util;

import com.parking.parking_backend.dto.BookingResponse;
import com.parking.parking_backend.model.Booking;

import java.time.LocalDateTime;

public class BookingMapper {

    public static BookingResponse toResponse(Booking b) {
        LocalDateTime now = LocalDateTime.now();

        BookingResponse r = new BookingResponse(
                b.getId(),
                b.getSlot().getSlotNumber(),
                b.getVehicle().getVehicleNumber(),
                b.getEntryTime(),
                b.getExitTime(),
                b.getStatus(),
                b.getFare(),
                b.getExpectedExitTime()
        );

        boolean active = "ACTIVE".equals(b.getStatus());
        boolean upcoming = active && b.getEntryTime().isAfter(now);

        r.setUpcoming(upcoming);
        r.setOverstay(active && !upcoming && b.getExpectedExitTime() != null
                && b.getExpectedExitTime().isBefore(now));
        r.setEstimatedFare(b.getEstimatedFare());
        r.setRating(b.getRating());
        r.setFeedback(b.getFeedback());
        return r;
    }
}
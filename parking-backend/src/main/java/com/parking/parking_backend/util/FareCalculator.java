package com.parking.parking_backend.util;

import com.parking.parking_backend.model.Booking;

import java.time.Duration;
import java.time.LocalDateTime;

public class FareCalculator {

    // hours rounded up, minimum 1 hour
    public static double billableHours(LocalDateTime from, LocalDateTime to) {
        long minutes = Math.max(0, Duration.between(from, to).toMinutes());
        return Math.max(1, Math.ceil(minutes / 60.0));
    }

    public static double estimate(String vehicleType, LocalDateTime from, LocalDateTime to) {
        return billableHours(from, to) * PricingConfig.getRateForType(vehicleType);
    }

    // Final fare: time from check-in to the actual exit. A customer reservation never pays
    // less than the reserved amount; leaving late costs extra hours.
    public static double finalFare(Booking booking, LocalDateTime exitTime) {
        double actual = estimate(booking.getVehicle().getVehicleType(), booking.getEntryTime(), exitTime);
        if (booking.getEstimatedFare() != null) {
            return Math.max(actual, booking.getEstimatedFare());
        }
        return actual;
    }
}
package com.parking.parking_backend.util;

import com.parking.parking_backend.model.Booking;
import com.parking.parking_backend.model.Vehicle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FareCalculatorTest {

    @Test
    @DisplayName("Billable hours should round up with a minimum of 1 hour")
    void testBillableHours() {
        LocalDateTime start = LocalDateTime.of(2026, 10, 25, 10, 0);

        // 30 minutes -> 1 hour minimum
        assertEquals(1.0, FareCalculator.billableHours(start, start.plusMinutes(30)));

        // Exactly 60 minutes -> 1 hour
        assertEquals(1.0, FareCalculator.billableHours(start, start.plusMinutes(60)));

        // 61 minutes -> rounds up to 2 hours
        assertEquals(2.0, FareCalculator.billableHours(start, start.plusMinutes(61)));

        // 120 minutes -> 2 hours
        assertEquals(2.0, FareCalculator.billableHours(start, start.plusMinutes(120)));

        // 125 minutes -> 3 hours
        assertEquals(3.0, FareCalculator.billableHours(start, start.plusMinutes(125)));
    }

    @Test
    @DisplayName("Estimates should reflect vehicle rate per hour")
    void testEstimatePricing() {
        LocalDateTime start = LocalDateTime.of(2026, 10, 25, 10, 0);
        LocalDateTime end2h = start.plusHours(2);

        // Car: 30/hr * 2 = 60
        assertEquals(60.0, FareCalculator.estimate("Car", start, end2h));

        // Bike: 10/hr * 2 = 20
        assertEquals(20.0, FareCalculator.estimate("Bike", start, end2h));

        // Truck: 50/hr * 2 = 100
        assertEquals(100.0, FareCalculator.estimate("Truck", start, end2h));

        // Default: 20/hr * 2 = 40
        assertEquals(40.0, FareCalculator.estimate("Unknown", start, end2h));
    }

    @Test
    @DisplayName("Final fare should charge extra for overstays")
    void testFinalFareOverstay() {
        LocalDateTime reservedStart = LocalDateTime.of(2026, 10, 25, 10, 0);
        LocalDateTime reservedEnd = reservedStart.plusHours(2);

        Vehicle car = new Vehicle();
        car.setVehicleType("Car");

        Booking booking = new Booking();
        booking.setVehicle(car);
        booking.setEntryTime(reservedStart);
        booking.setExpectedExitTime(reservedEnd);
        booking.setEstimatedFare(60.0); // 2 hours @ 30

        // Exits at 3.5 hours -> rounds up to 4 hours (4 * 30 = 120)
        LocalDateTime actualExit = reservedStart.plusHours(3).plusMinutes(30);
        double fare = FareCalculator.finalFare(booking, actualExit);

        assertEquals(120.0, fare);
    }

    @Test
    @DisplayName("Final fare should preserve estimated minimum if customer leaves early")
    void testFinalFareEarlyExit() {
        LocalDateTime reservedStart = LocalDateTime.of(2026, 10, 25, 10, 0);
        LocalDateTime reservedEnd = reservedStart.plusHours(3);

        Vehicle car = new Vehicle();
        car.setVehicleType("Car");

        Booking booking = new Booking();
        booking.setVehicle(car);
        booking.setEntryTime(reservedStart);
        booking.setExpectedExitTime(reservedEnd);
        booking.setEstimatedFare(90.0); // 3 hours reserved

        // Customer leaves after 30 minutes (actual would be 30.0)
        LocalDateTime actualExit = reservedStart.plusMinutes(30);
        double fare = FareCalculator.finalFare(booking, actualExit);

        // Retains the committed reservation minimum of 90.0
        assertEquals(90.0, fare);
    }
}

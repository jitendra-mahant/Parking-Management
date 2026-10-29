package com.parking.parking_backend.service;

import com.parking.parking_backend.model.Booking;
import com.parking.parking_backend.model.Slot;
import com.parking.parking_backend.model.Vehicle;
import com.parking.parking_backend.repository.BookingRepository;
import com.parking.parking_backend.repository.SlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SlotAvailabilityServiceTest {

    private SlotAvailabilityService slotAvailabilityService;
    private final List<Booking> activeBookings = new ArrayList<>();
    private final List<Slot> carSlots = new ArrayList<>();

    private Slot slotA1;
    private Slot slotA2;
    private Vehicle vehicle1;
    private Vehicle vehicle2;

    @BeforeEach
    void setup() throws Exception {
        slotAvailabilityService = new SlotAvailabilityService();
        activeBookings.clear();
        carSlots.clear();

        slotA1 = new Slot();
        slotA1.setId(1L);
        slotA1.setSlotNumber("A1");
        slotA1.setVehicleType("Car");

        slotA2 = new Slot();
        slotA2.setId(2L);
        slotA2.setSlotNumber("A2");
        slotA2.setVehicleType("Car");

        carSlots.add(slotA1);
        carSlots.add(slotA2);

        vehicle1 = new Vehicle();
        vehicle1.setId(101L);
        vehicle1.setVehicleNumber("MH12AB1111");
        vehicle1.setVehicleType("Car");

        vehicle2 = new Vehicle();
        vehicle2.setId(102L);
        vehicle2.setVehicleNumber("MH12CD2222");
        vehicle2.setVehicleType("Car");

        BookingRepository bookingRepoProxy = (BookingRepository) Proxy.newProxyInstance(
                BookingRepository.class.getClassLoader(),
                new Class<?>[]{BookingRepository.class},
                (proxy, method, args) -> {
                    if ("findByStatus".equals(method.getName())) {
                        return activeBookings;
                    }
                    return null;
                }
        );

        SlotRepository slotRepoProxy = (SlotRepository) Proxy.newProxyInstance(
                SlotRepository.class.getClassLoader(),
                new Class<?>[]{SlotRepository.class},
                (proxy, method, args) -> {
                    if ("findByVehicleType".equals(method.getName())) {
                        return carSlots;
                    }
                    return null;
                }
        );

        Field fBookingRepo = SlotAvailabilityService.class.getDeclaredField("bookingRepository");
        fBookingRepo.setAccessible(true);
        fBookingRepo.set(slotAvailabilityService, bookingRepoProxy);

        Field fSlotRepo = SlotAvailabilityService.class.getDeclaredField("slotRepository");
        fSlotRepo.setAccessible(true);
        fSlotRepo.set(slotAvailabilityService, slotRepoProxy);
    }

    @Test
    @DisplayName("Should detect slot conflicts when booking windows overlap")
    void testSlotOverlapDetection() {
        LocalDateTime baseTime = LocalDateTime.now().plusDays(1);
        LocalDateTime bookedStart = baseTime.plusHours(2);
        LocalDateTime bookedEnd = baseTime.plusHours(4);

        Booking existing = new Booking();
        existing.setId(10L);
        existing.setSlot(slotA1);
        existing.setVehicle(vehicle1);
        existing.setEntryTime(bookedStart);
        existing.setExpectedExitTime(bookedEnd);
        existing.setStatus("ACTIVE");

        activeBookings.add(existing);

        // Attempt 1: Completely inside existing window [bookedStart + 1h, bookedStart + 2h) -> Overlaps
        assertFalse(slotAvailabilityService.isSlotFree(1L, bookedStart.plusHours(1), bookedStart.plusHours(2), null));

        // Attempt 2: Overlapping start [bookedStart - 1h, bookedStart + 1h) -> Overlaps
        assertFalse(slotAvailabilityService.isSlotFree(1L, bookedStart.minusHours(1), bookedStart.plusHours(1), null));

        // Attempt 3: Completely after [bookedEnd + 1h, bookedEnd + 3h) -> Free
        assertTrue(slotAvailabilityService.isSlotFree(1L, bookedEnd.plusHours(1), bookedEnd.plusHours(3), null));

        // Attempt 4: Completely before [bookedStart - 3h, bookedStart - 1h) -> Free
        assertTrue(slotAvailabilityService.isSlotFree(1L, bookedStart.minusHours(3), bookedStart.minusHours(1), null));

        // Attempt 5: Different slot (slotA2) during the same time -> Free
        assertTrue(slotAvailabilityService.isSlotFree(2L, bookedStart, bookedEnd, null));
    }

    @Test
    @DisplayName("Should prevent the same vehicle from holding two overlapping reservations")
    void testVehicleConflictDetection() {
        LocalDateTime baseTime = LocalDateTime.now().plusDays(1);
        LocalDateTime bookedStart = baseTime.plusHours(2);
        LocalDateTime bookedEnd = baseTime.plusHours(4);

        Booking existing = new Booking();
        existing.setId(11L);
        existing.setSlot(slotA1);
        existing.setVehicle(vehicle1);
        existing.setEntryTime(bookedStart);
        existing.setExpectedExitTime(bookedEnd);
        existing.setStatus("ACTIVE");

        activeBookings.add(existing);

        // Vehicle 1 tries to reserve another slot during overlapping window -> False
        assertFalse(slotAvailabilityService.isVehicleFree(101L, bookedStart.plusHours(1), bookedStart.plusHours(3), null));

        // Vehicle 2 (different vehicle) booking same window -> True
        assertTrue(slotAvailabilityService.isVehicleFree(102L, bookedStart.plusHours(1), bookedStart.plusHours(3), null));
    }

    @Test
    @DisplayName("Should find available slots filtering out occupied ones")
    void testFindFreeSlots() {
        LocalDateTime baseTime = LocalDateTime.now().plusDays(1);
        LocalDateTime requestedStart = baseTime.plusHours(1);
        LocalDateTime requestedEnd = baseTime.plusHours(3);

        Booking bookingA1 = new Booking();
        bookingA1.setId(12L);
        bookingA1.setSlot(slotA1);
        bookingA1.setVehicle(vehicle1);
        bookingA1.setEntryTime(requestedStart);
        bookingA1.setExpectedExitTime(requestedEnd);
        bookingA1.setStatus("ACTIVE");

        activeBookings.add(bookingA1);

        List<Slot> freeSlots = slotAvailabilityService.findFreeSlots("Car", requestedStart, requestedEnd);

        // Only slot A2 should be returned
        assertEquals(1, freeSlots.size());
        assertEquals("A2", freeSlots.get(0).getSlotNumber());
    }
}

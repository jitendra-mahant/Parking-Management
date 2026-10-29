package com.parking.parking_backend.service;

import com.parking.parking_backend.dto.RevenueReport;
import com.parking.parking_backend.model.Booking;
import com.parking.parking_backend.repository.BookingRepository;
import com.parking.parking_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AdminServiceTest {

    private AdminService adminService;
    private final List<Booking> allBookings = new ArrayList<>();

    @BeforeEach
    void setup() throws Exception {
        adminService = new AdminService();
        allBookings.clear();

        BookingRepository bookingRepoProxy = (BookingRepository) Proxy.newProxyInstance(
                BookingRepository.class.getClassLoader(),
                new Class<?>[]{BookingRepository.class},
                (proxy, method, args) -> {
                    if ("findAll".equals(method.getName())) {
                        return allBookings;
                    }
                    return null;
                }
        );

        UserRepository userRepoProxy = (UserRepository) Proxy.newProxyInstance(
                UserRepository.class.getClassLoader(),
                new Class<?>[]{UserRepository.class},
                (proxy, method, args) -> null
        );

        Field fBooking = AdminService.class.getDeclaredField("bookingRepository");
        fBooking.setAccessible(true);
        fBooking.set(adminService, bookingRepoProxy);

        Field fUser = AdminService.class.getDeclaredField("userRepository");
        fUser.setAccessible(true);
        fUser.set(adminService, userRepoProxy);
    }

    @Test
    @DisplayName("Revenue report should calculate totals accurately")
    void testRevenueReport() {
        Booking b1 = new Booking();
        b1.setStatus("COMPLETED");
        b1.setFare(120.0);

        Booking b2 = new Booking();
        b2.setStatus("COMPLETED");
        b2.setFare(80.0);

        Booking b3 = new Booking();
        b3.setStatus("ACTIVE");

        allBookings.add(b1);
        allBookings.add(b2);
        allBookings.add(b3);

        RevenueReport report = adminService.getRevenueReport();

        assertEquals(3, report.getTotalBookings());
        assertEquals(1, report.getActiveBookings());
        assertEquals(2, report.getCompletedBookings());
        assertEquals(200.0, report.getTotalRevenue());
    }

    @Test
    @DisplayName("Revenue chart should populate 7-day rolling window with accurate fares")
    void testRevenueChart() {
        LocalDate today = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        Booking bToday = new Booking();
        bToday.setStatus("COMPLETED");
        bToday.setFare(150.0);
        bToday.setExitTime(today.atTime(14, 30));

        Booking bYesterday = new Booking();
        bYesterday.setStatus("COMPLETED");
        bYesterday.setFare(90.0);
        bYesterday.setExitTime(today.minusDays(1).atTime(11, 0));

        allBookings.add(bToday);
        allBookings.add(bYesterday);

        List<Map<String, Object>> chart = adminService.getRevenueChart();

        assertEquals(7, chart.size(), "Chart must return 7 days");

        String todayStr = today.format(fmt);
        String yestStr = today.minusDays(1).format(fmt);

        double todayRevenue = chart.stream()
                .filter(m -> todayStr.equals(m.get("date")))
                .mapToDouble(m -> (Double) m.get("revenue"))
                .findFirst().orElse(-1.0);

        double yestRevenue = chart.stream()
                .filter(m -> yestStr.equals(m.get("date")))
                .mapToDouble(m -> (Double) m.get("revenue"))
                .findFirst().orElse(-1.0);

        assertEquals(150.0, todayRevenue);
        assertEquals(90.0, yestRevenue);
    }
}

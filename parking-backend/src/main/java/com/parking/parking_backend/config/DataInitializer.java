package com.parking.parking_backend.config;

import com.parking.parking_backend.model.Slot;
import com.parking.parking_backend.model.User;
import com.parking.parking_backend.model.Vehicle;
import com.parking.parking_backend.repository.SlotRepository;
import com.parking.parking_backend.repository.UserRepository;
import com.parking.parking_backend.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SlotRepository slotRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // 1. Seed admin if none exists
        if (userRepository.findAll().stream().noneMatch(u -> "admin".equalsIgnoreCase(u.getRole()))) {
            User admin = new User();
            admin.setName("Admin Manager");
            admin.setEmail("admin@parkease.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setPhone("9876543210");
            admin.setRole("admin");
            admin.setCreatedAt(LocalDateTime.now());
            userRepository.save(admin);
        }

        // 2. Seed staff if none exists
        if (userRepository.findAll().stream().noneMatch(u -> "staff".equalsIgnoreCase(u.getRole()))) {
            User staff = new User();
            staff.setName("Staff Operator");
            staff.setEmail("staff@parkease.com");
            staff.setPassword(passwordEncoder.encode("staff123"));
            staff.setPhone("9876543211");
            staff.setRole("staff");
            staff.setCreatedAt(LocalDateTime.now());
            userRepository.save(staff);
        }

        // 3. Seed demo customer if none exists
        User customer = userRepository.findByEmail("customer@parkease.com").orElse(null);
        if (customer == null && userRepository.findAll().stream().noneMatch(u -> "customer".equalsIgnoreCase(u.getRole()))) {
            customer = new User();
            customer.setName("John Customer");
            customer.setEmail("customer@parkease.com");
            customer.setPassword(passwordEncoder.encode("customer123"));
            customer.setPhone("9876543212");
            customer.setRole("customer");
            customer.setCreatedAt(LocalDateTime.now());
            customer = userRepository.save(customer);

            // Seed demo vehicle
            Vehicle vehicle = new Vehicle();
            vehicle.setVehicleNumber("MH12AB1234");
            vehicle.setVehicleType("Car");
            vehicle.setModel("Honda City");
            vehicle.setOwner(customer);
            vehicleRepository.save(vehicle);
        }

        // 4. Seed 24 standard slots if slot table is empty
        if (slotRepository.count() == 0) {
            // 12 Car slots: A1 - A12
            for (int i = 1; i <= 12; i++) {
                Slot slot = new Slot();
                slot.setSlotNumber("A" + i);
                slot.setVehicleType("Car");
                slot.setOccupied(false);
                slotRepository.save(slot);
            }
            // 8 Bike slots: B1 - B8
            for (int i = 1; i <= 8; i++) {
                Slot slot = new Slot();
                slot.setSlotNumber("B" + i);
                slot.setVehicleType("Bike");
                slot.setOccupied(false);
                slotRepository.save(slot);
            }
            // 4 Truck slots: T1 - T4
            for (int i = 1; i <= 4; i++) {
                Slot slot = new Slot();
                slot.setSlotNumber("T" + i);
                slot.setVehicleType("Truck");
                slot.setOccupied(false);
                slotRepository.save(slot);
            }
        }
    }
}

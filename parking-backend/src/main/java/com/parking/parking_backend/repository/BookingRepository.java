package com.parking.parking_backend.repository;

import com.parking.parking_backend.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByCustomerEmailOrderByEntryTimeDesc(String email);
    List<Booking> findByStatus(String status);
}
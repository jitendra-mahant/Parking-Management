package com.parking.parking_backend.dto;

import java.time.LocalDateTime;

public class BookingResponse {
    private Long id;
    private String slotNumber;
    private String vehicleNumber;
    private LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private String status;
    private Double fare;
    private LocalDateTime expectedExitTime;
    private boolean overstay;
    private Integer rating;
    private String feedback;

    public BookingResponse(Long id, String slotNumber, String vehicleNumber, LocalDateTime entryTime,
                            LocalDateTime exitTime, String status, Double fare, LocalDateTime expectedExitTime) {
        this.id = id;
        this.slotNumber = slotNumber;
        this.vehicleNumber = vehicleNumber;
        this.entryTime = entryTime;
        this.exitTime = exitTime;
        this.status = status;
        this.fare = fare;
        this.expectedExitTime = expectedExitTime;
    }

    public Long getId() { return id; }
    public String getSlotNumber() { return slotNumber; }
    public String getVehicleNumber() { return vehicleNumber; }
    public LocalDateTime getEntryTime() { return entryTime; }
    public LocalDateTime getExitTime() { return exitTime; }
    public String getStatus() { return status; }
    public Double getFare() { return fare; }
    public LocalDateTime getExpectedExitTime() { return expectedExitTime; }

    public boolean isOverstay() { return overstay; }
    public void setOverstay(boolean overstay) { this.overstay = overstay; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
}
package com.parking.parking_backend.dto;

public class RevenueReport {
    private long totalBookings;
    private long activeBookings;
    private long completedBookings;
    private double totalRevenue;

    public RevenueReport(long totalBookings, long activeBookings, long completedBookings, double totalRevenue) {
        this.totalBookings = totalBookings;
        this.activeBookings = activeBookings;
        this.completedBookings = completedBookings;
        this.totalRevenue = totalRevenue;
    }

    public long getTotalBookings() { return totalBookings; }
    public long getActiveBookings() { return activeBookings; }
    public long getCompletedBookings() { return completedBookings; }
    public double getTotalRevenue() { return totalRevenue; }
}
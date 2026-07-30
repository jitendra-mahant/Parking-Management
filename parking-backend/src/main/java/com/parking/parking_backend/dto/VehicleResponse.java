package com.parking.parking_backend.dto;

public class VehicleResponse {
    private Long id;
    private String vehicleNumber;
    private String vehicleType;
    private String model;

    public VehicleResponse(Long id, String vehicleNumber, String vehicleType, String model) {
        this.id = id;
        this.vehicleNumber = vehicleNumber;
        this.vehicleType = vehicleType;
        this.model = model;
    }

    public Long getId() { return id; }
    public String getVehicleNumber() { return vehicleNumber; }
    public String getVehicleType() { return vehicleType; }
    public String getModel() { return model; }
}
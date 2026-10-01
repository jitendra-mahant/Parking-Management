package com.parking.parking_backend.util;

import java.util.Map;

public class PricingConfig {

    private static final Map<String, Double> RATES_PER_HOUR = Map.of(
            "Car", 30.0,
            "Bike", 10.0,
            "Truck", 50.0
    );

    private static final double DEFAULT_RATE = 20.0;

    public static double getRateForType(String vehicleType) {
        return RATES_PER_HOUR.getOrDefault(vehicleType, DEFAULT_RATE);
    }
}
package com.parking.client.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class VehicleApiService {

    private static final Gson gson = new Gson();

    public static List<VehicleData> getMyVehicles() throws IOException {
        String response = ApiClient.get("/vehicles");
        JsonArray array = gson.fromJson(response, JsonArray.class);

        List<VehicleData> vehicles = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            JsonObject obj = array.get(i).getAsJsonObject();
            vehicles.add(new VehicleData(
                    obj.get("id").getAsLong(),
                    obj.get("vehicleNumber").getAsString(),
                    obj.get("vehicleType").getAsString(),
                    obj.has("model") && !obj.get("model").isJsonNull() ? obj.get("model").getAsString() : ""
            ));
        }
        return vehicles;
    }

    public static void addVehicle(String vehicleNumber, String vehicleType, String model) throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("vehicleNumber", vehicleNumber);
        body.addProperty("vehicleType", vehicleType);
        body.addProperty("model", model);

        ApiClient.post("/vehicles", gson.toJson(body));
    }

    public static void deleteVehicle(Long id) throws IOException {
        ApiClient.delete("/vehicles/" + id);
    }

    public static class VehicleData {
        public Long id;
        public String vehicleNumber;
        public String vehicleType;
        public String model;

        public VehicleData(Long id, String vehicleNumber, String vehicleType, String model) {
            this.id = id;
            this.vehicleNumber = vehicleNumber;
            this.vehicleType = vehicleType;
            this.model = model;
        }
    }
}
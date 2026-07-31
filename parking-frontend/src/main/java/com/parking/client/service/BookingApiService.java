package com.parking.client.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class BookingApiService {

    private static final Gson gson = new Gson();

    public static List<SlotData> getAvailableSlots(String vehicleType) throws IOException {
        String response = ApiClient.get("/bookings/available-slots?vehicleType=" + vehicleType);
        JsonArray array = gson.fromJson(response, JsonArray.class);

        List<SlotData> slots = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            JsonObject obj = array.get(i).getAsJsonObject();
            slots.add(new SlotData(
                    obj.get("id").getAsLong(),
                    obj.get("slotNumber").getAsString(),
                    obj.get("vehicleType").getAsString()
            ));
        }
        return slots;
    }

    public static BookingData bookSlot(Long vehicleId, String vehicleType) throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("vehicleId", vehicleId);
        body.addProperty("vehicleType", vehicleType);

        String response = ApiClient.post("/bookings", gson.toJson(body));
        return parseBooking(gson.fromJson(response, JsonObject.class));
    }

    public static List<BookingData> getMyBookings() throws IOException {
        String response = ApiClient.get("/bookings");
        JsonArray array = gson.fromJson(response, JsonArray.class);

        List<BookingData> bookings = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            bookings.add(parseBooking(array.get(i).getAsJsonObject()));
        }
        return bookings;
    }

    public static BookingData cancelBooking(Long bookingId) throws IOException {
        String response = ApiClient.put("/bookings/" + bookingId + "/cancel", "{}");
        return parseBooking(gson.fromJson(response, JsonObject.class));
    }

    public static BookingData completeBooking(Long bookingId) throws IOException {
        String response = ApiClient.put("/bookings/" + bookingId + "/complete", "{}");
        return parseBooking(gson.fromJson(response, JsonObject.class));
    }

    private static BookingData parseBooking(JsonObject obj) {
        return new BookingData(
                obj.get("id").getAsLong(),
                obj.get("slotNumber").getAsString(),
                obj.get("vehicleNumber").getAsString(),
                obj.get("entryTime").getAsString(),
                obj.has("exitTime") && !obj.get("exitTime").isJsonNull() ? obj.get("exitTime").getAsString() : null,
                obj.get("status").getAsString(),
                obj.has("fare") && !obj.get("fare").isJsonNull() ? obj.get("fare").getAsDouble() : null
        );
    }

    public static class SlotData {
        public Long id;
        public String slotNumber;
        public String vehicleType;

        public SlotData(Long id, String slotNumber, String vehicleType) {
            this.id = id;
            this.slotNumber = slotNumber;
            this.vehicleType = vehicleType;
        }
    }

    public static class BookingData {
        public Long id;
        public String slotNumber;
        public String vehicleNumber;
        public String entryTime;
        public String exitTime;
        public String status;
        public Double fare;

        public BookingData(Long id, String slotNumber, String vehicleNumber, String entryTime,
                            String exitTime, String status, Double fare) {
            this.id = id;
            this.slotNumber = slotNumber;
            this.vehicleNumber = vehicleNumber;
            this.entryTime = entryTime;
            this.exitTime = exitTime;
            this.status = status;
            this.fare = fare;
        }
    }
}
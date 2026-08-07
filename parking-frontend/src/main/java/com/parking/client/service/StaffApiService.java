package com.parking.client.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class StaffApiService {

    private static final Gson gson = new Gson();

    public static BookingApiService.BookingData recordEntry(String vehicleNumber, String vehicleType) throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("vehicleNumber", vehicleNumber);
        body.addProperty("vehicleType", vehicleType);

        String response = ApiClient.post("/staff/entry", gson.toJson(body));
        return parseBooking(gson.fromJson(response, JsonObject.class));
    }

    public static BookingApiService.BookingData recordExit(Long bookingId) throws IOException {
        String response = ApiClient.put("/staff/exit/" + bookingId, "{}");
        return parseBooking(gson.fromJson(response, JsonObject.class));
    }

    public static List<BookingApiService.BookingData> getAllActiveBookings() throws IOException {
        String response = ApiClient.get("/staff/active-bookings");
        JsonArray array = gson.fromJson(response, JsonArray.class);

        List<BookingApiService.BookingData> bookings = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            bookings.add(parseBooking(array.get(i).getAsJsonObject()));
        }
        return bookings;
    }

    public static List<SlotInfo> getAllSlots() throws IOException {
        String response = ApiClient.get("/staff/slots");
        JsonArray array = gson.fromJson(response, JsonArray.class);

        List<SlotInfo> slots = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            JsonObject obj = array.get(i).getAsJsonObject();
            slots.add(new SlotInfo(
                    obj.get("id").getAsLong(),
                    obj.get("slotNumber").getAsString(),
                    obj.get("vehicleType").getAsString(),
                    obj.get("occupied").getAsBoolean()
            ));
        }
        return slots;
    }

    private static BookingApiService.BookingData parseBooking(JsonObject obj) {
        return new BookingApiService.BookingData(
                obj.get("id").getAsLong(),
                obj.get("slotNumber").getAsString(),
                obj.get("vehicleNumber").getAsString(),
                obj.get("entryTime").getAsString(),
                obj.has("exitTime") && !obj.get("exitTime").isJsonNull() ? obj.get("exitTime").getAsString() : null,
                obj.get("status").getAsString(),
                obj.has("fare") && !obj.get("fare").isJsonNull() ? obj.get("fare").getAsDouble() : null
        );
    }

    public static class SlotInfo {
        public Long id;
        public String slotNumber;
        public String vehicleType;
        public boolean occupied;

        public SlotInfo(Long id, String slotNumber, String vehicleType, boolean occupied) {
            this.id = id;
            this.slotNumber = slotNumber;
            this.vehicleType = vehicleType;
            this.occupied = occupied;
        }
    }
    public static QuickStats getQuickStats() throws IOException {
        String response = ApiClient.get("/staff/quick-stats");
        JsonObject obj = gson.fromJson(response, JsonObject.class);

        return new QuickStats(
                obj.get("totalSlots").getAsLong(),
                obj.get("occupiedSlots").getAsLong(),
                obj.get("availableSlots").getAsLong(),
                obj.get("todayEntries").getAsLong(),
                obj.get("todayExits").getAsLong()
        );
    }

    public static class QuickStats {
        public long totalSlots;
        public long occupiedSlots;
        public long availableSlots;
        public long todayEntries;
        public long todayExits;

        public QuickStats(long totalSlots, long occupiedSlots, long availableSlots, long todayEntries, long todayExits) {
            this.totalSlots = totalSlots;
            this.occupiedSlots = occupiedSlots;
            this.availableSlots = availableSlots;
            this.todayEntries = todayEntries;
            this.todayExits = todayExits;
        }
    }
}
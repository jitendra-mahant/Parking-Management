package com.parking.client.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class AdminApiService {

    private static final Gson gson = new Gson();

    public static void createStaff(String name, String email, String password, String phone) throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("name", name);
        body.addProperty("email", email);
        body.addProperty("password", password);
        body.addProperty("phone", phone);

        ApiClient.post("/admin/staff", gson.toJson(body));
    }

    public static List<StaffData> getAllStaff() throws IOException {
        String response = ApiClient.get("/admin/staff");
        JsonArray array = gson.fromJson(response, JsonArray.class);

        List<StaffData> staffList = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            JsonObject obj = array.get(i).getAsJsonObject();
            staffList.add(new StaffData(
                    obj.get("id").getAsLong(),
                    obj.get("name").getAsString(),
                    obj.get("email").getAsString(),
                    obj.has("phone") && !obj.get("phone").isJsonNull() ? obj.get("phone").getAsString() : ""
            ));
        }
        return staffList;
    }

    public static void deleteStaff(Long id) throws IOException {
        ApiClient.delete("/admin/staff/" + id);
    }

    public static List<BookingApiService.BookingData> getAllBookings() throws IOException {
        String response = ApiClient.get("/admin/bookings");
        JsonArray array = gson.fromJson(response, JsonArray.class);

        List<BookingApiService.BookingData> bookings = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            bookings.add(BookingApiService.parseBooking(array.get(i).getAsJsonObject()));
        }
        return bookings;
    }

    public static RevenueData getRevenueReport() throws IOException {
        String response = ApiClient.get("/admin/revenue");
        JsonObject obj = gson.fromJson(response, JsonObject.class);

        return new RevenueData(
                obj.get("totalBookings").getAsLong(),
                obj.get("activeBookings").getAsLong(),
                obj.get("completedBookings").getAsLong(),
                obj.get("totalRevenue").getAsDouble()
        );
    }

    public static List<DailyRevenueData> getRevenueChart() throws IOException {
        String response = ApiClient.get("/admin/revenue-chart");
        JsonArray array = gson.fromJson(response, JsonArray.class);

        List<DailyRevenueData> list = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            JsonObject obj = array.get(i).getAsJsonObject();
            list.add(new DailyRevenueData(obj.get("date").getAsString(), obj.get("revenue").getAsDouble()));
        }
        return list;
    }

    public static class StaffData {
        public Long id;
        public String name;
        public String email;
        public String phone;

        public StaffData(Long id, String name, String email, String phone) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.phone = phone;
        }
    }

    public static class RevenueData {
        public long totalBookings;
        public long activeBookings;
        public long completedBookings;
        public double totalRevenue;

        public RevenueData(long totalBookings, long activeBookings, long completedBookings, double totalRevenue) {
            this.totalBookings = totalBookings;
            this.activeBookings = activeBookings;
            this.completedBookings = completedBookings;
            this.totalRevenue = totalRevenue;
        }
    }

    public static class DailyRevenueData {
        public String date;
        public double revenue;

        public DailyRevenueData(String date, double revenue) {
            this.date = date;
            this.revenue = revenue;
        }
    }
}
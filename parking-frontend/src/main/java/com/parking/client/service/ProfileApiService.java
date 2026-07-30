package com.parking.client.service;

import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class ProfileApiService {

    private static final Gson gson = new Gson();

    public static ProfileData getProfile() throws IOException {
        String response = ApiClient.get("/profile");
        JsonObject json = gson.fromJson(response, JsonObject.class);

        return new ProfileData(
                json.get("name").getAsString(),
                json.get("email").getAsString(),
                json.get("phone").getAsString(),
                json.get("role").getAsString()
        );
    }

    public static ProfileData updateProfile(String name, String phone) throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("name", name);
        body.addProperty("phone", phone);

        String response = ApiClient.put("/profile", gson.toJson(body));
        JsonObject json = gson.fromJson(response, JsonObject.class);

        return new ProfileData(
                json.get("name").getAsString(),
                json.get("email").getAsString(),
                json.get("phone").getAsString(),
                json.get("role").getAsString()
        );
    }

    public static String changePassword(String currentPassword, String newPassword) throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("currentPassword", currentPassword);
        body.addProperty("newPassword", newPassword);

        return ApiClient.put("/profile/change-password", gson.toJson(body));
    }

    public static class ProfileData {
        public String name;
        public String email;
        public String phone;
        public String role;

        public ProfileData(String name, String email, String phone, String role) {
            this.name = name;
            this.email = email;
            this.phone = phone;
            this.role = role;
        }
    }
}
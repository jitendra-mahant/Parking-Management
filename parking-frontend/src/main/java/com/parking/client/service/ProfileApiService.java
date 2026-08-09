package com.parking.client.service;

import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class ProfileApiService {

    private static final Gson gson = new Gson();

    public static ProfileData getProfile() throws IOException {
        String response = ApiClient.get("/profile");
        return parseProfile(gson.fromJson(response, JsonObject.class));
    }

    public static ProfileData updateProfile(String name, String phone, String address) throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("name", name);
        body.addProperty("phone", phone);
        body.addProperty("address", address);

        String response = ApiClient.put("/profile", gson.toJson(body));
        return parseProfile(gson.fromJson(response, JsonObject.class));
    }

    public static String changePassword(String currentPassword, String newPassword) throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("currentPassword", currentPassword);
        body.addProperty("newPassword", newPassword);

        return ApiClient.put("/profile/change-password", gson.toJson(body));
    }

    public static void updatePhoto(String photoBase64) throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("photoBase64", photoBase64);

        ApiClient.put("/profile/photo", gson.toJson(body));
    }

    private static ProfileData parseProfile(JsonObject json) {
        return new ProfileData(
                json.get("name").getAsString(),
                json.get("email").getAsString(),
                json.has("phone") && !json.get("phone").isJsonNull() ? json.get("phone").getAsString() : "",
                json.get("role").getAsString(),
                json.has("address") && !json.get("address").isJsonNull() ? json.get("address").getAsString() : "",
                json.has("photoBase64") && !json.get("photoBase64").isJsonNull() ? json.get("photoBase64").getAsString() : null
        );
    }

    public static class ProfileData {
        public String name;
        public String email;
        public String phone;
        public String role;
        public String address;
        public String photoBase64;

        public ProfileData(String name, String email, String phone, String role, String address, String photoBase64) {
            this.name = name;
            this.email = email;
            this.phone = phone;
            this.role = role;
            this.address = address;
            this.photoBase64 = photoBase64;
        }
    }
}
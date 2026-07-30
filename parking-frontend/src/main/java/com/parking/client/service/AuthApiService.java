package com.parking.client.service;

import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class AuthApiService {

    private static final Gson gson = new Gson();

    public static AuthResult login(String email, String password) throws IOException {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("email", email);
        requestBody.addProperty("password", password);

        String response = ApiClient.post("/auth/login", gson.toJson(requestBody));

        JsonObject json = gson.fromJson(response, JsonObject.class);

        return new AuthResult(
                json.get("token").getAsString(),
                json.get("name").getAsString(),
                json.get("email").getAsString(),
                json.get("role").getAsString()
        );
    }

    public static AuthResult signup(String name, String email, String password, String phone) throws IOException {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("name", name);
        requestBody.addProperty("email", email);
        requestBody.addProperty("password", password);
        requestBody.addProperty("phone", phone);

        String response = ApiClient.post("/auth/signup", gson.toJson(requestBody));

        JsonObject json = gson.fromJson(response, JsonObject.class);

        return new AuthResult(
                json.get("token").getAsString(),
                json.get("name").getAsString(),
                json.get("email").getAsString(),
                json.get("role").getAsString()
        );
    }

    // Simple result holder class
    public static class AuthResult {
        public String token;
        public String name;
        public String email;
        public String role;

        public AuthResult(String token, String name, String email, String role) {
            this.token = token;
            this.name = name;
            this.email = email;
            this.role = role;
        }
    }

    public static String forgotPassword(String email) throws IOException {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("email", email);
        return ApiClient.post("/auth/forgot-password", gson.toJson(requestBody));
    }

    public static String verifyOtp(String email, String otp) throws IOException {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("email", email);
        requestBody.addProperty("otp", otp);
        return ApiClient.post("/auth/verify-otp", gson.toJson(requestBody));
    }

    public static String resetPassword(String email, String newPassword) throws IOException {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("email", email);
        requestBody.addProperty("newPassword", newPassword);
        return ApiClient.post("/auth/reset-password", gson.toJson(requestBody));
    }
}
package com.parking.client.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;

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
}
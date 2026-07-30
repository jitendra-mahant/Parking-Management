package com.parking.client.service;

import java.io.IOException;

import com.parking.client.util.SessionManager;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ApiClient {

    private static final String BASE_URL = "http://localhost:8080/api";
    private static final OkHttpClient client = new OkHttpClient();
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public static String post(String endpoint, String jsonBody) throws IOException {
        RequestBody body = RequestBody.create(jsonBody, JSON);

        Request.Builder builder = new Request.Builder()
                .url(BASE_URL + endpoint)
                .post(body);

        addAuthHeader(builder);

        try (Response response = client.newCall(builder.build()).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new IOException(responseBody.isEmpty() ? "Request failed: " + response.code() : responseBody);
            }
            return responseBody;
        }
    }

    public static String get(String endpoint) throws IOException {
        Request.Builder builder = new Request.Builder()
                .url(BASE_URL + endpoint)
                .get();

        addAuthHeader(builder);

        try (Response response = client.newCall(builder.build()).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new IOException(responseBody.isEmpty() ? "Request failed: " + response.code() : responseBody);
            }
            return responseBody;
        }
    }

    public static String put(String endpoint, String jsonBody) throws IOException {
        RequestBody body = RequestBody.create(jsonBody, JSON);

        Request.Builder builder = new Request.Builder()
                .url(BASE_URL + endpoint)
                .put(body);

        addAuthHeader(builder);

        try (Response response = client.newCall(builder.build()).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new IOException(responseBody.isEmpty() ? "Request failed: " + response.code() : responseBody);
            }
            return responseBody;
        }
    }

    private static void addAuthHeader(Request.Builder builder) {
        String token = SessionManager.getToken();
        if (token != null) {
            builder.addHeader("Authorization", "Bearer " + token);
        }
    }
}
package com.parking.client.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class NotificationApiService {

    private static final Gson gson = new Gson();

    public static List<NotificationData> getMyNotifications() throws IOException {
        String response = ApiClient.get("/notifications");
        JsonArray array = gson.fromJson(response, JsonArray.class);

        List<NotificationData> notifications = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            JsonObject obj = array.get(i).getAsJsonObject();
            notifications.add(new NotificationData(
                    obj.get("id").getAsLong(),
                    obj.get("message").getAsString(),
                    obj.get("createdAt").getAsString(),
                    obj.get("read").getAsBoolean()
            ));
        }
        return notifications;
    }

    public static void markAsRead(Long id) throws IOException {
        ApiClient.put("/notifications/" + id + "/read", "{}");
    }

    public static class NotificationData {
        public Long id;
        public String message;
        public String createdAt;
        public boolean isRead;

        public NotificationData(Long id, String message, String createdAt, boolean isRead) {
            this.id = id;
            this.message = message;
            this.createdAt = createdAt;
            this.isRead = isRead;
        }
    }
}
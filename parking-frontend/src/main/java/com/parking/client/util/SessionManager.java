package com.parking.client.util;

public class SessionManager {

    private static String token;
    private static String name;
    private static String email;
    private static String role;

    public static void setSession(String token, String name, String email, String role) {
        SessionManager.token = token;
        SessionManager.name = name;
        SessionManager.email = email;
        SessionManager.role = role;
    }

    public static String getToken() { return token; }
    public static String getName() { return name; }
    public static String getEmail() { return email; }
    public static String getRole() { return role; }

    public static void clearSession() {
        token = null;
        name = null;
        email = null;
        role = null;
    }
}
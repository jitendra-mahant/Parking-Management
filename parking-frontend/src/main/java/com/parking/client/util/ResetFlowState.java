package com.parking.client.util;

// Temporarily holds the email while user moves through forgot-password screens
public class ResetFlowState {
    private static String email;

    public static void setEmail(String e) { email = e; }
    public static String getEmail() { return email; }
    public static void clear() { email = null; }
}
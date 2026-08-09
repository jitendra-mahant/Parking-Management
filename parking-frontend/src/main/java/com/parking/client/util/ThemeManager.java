package com.parking.client.util;

public class ThemeManager {
    private static boolean darkMode = false;

    public static boolean isDarkMode() {
        return darkMode;
    }

    public static void setDarkMode(boolean value) {
        darkMode = value;
    }
}
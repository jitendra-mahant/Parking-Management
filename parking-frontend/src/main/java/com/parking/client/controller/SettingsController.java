package com.parking.client.controller;

import java.net.URL;
import java.util.ResourceBundle;

import com.parking.client.MainApp;
import com.parking.client.util.SessionManager;
import com.parking.client.util.ThemeManager;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;

public class SettingsController implements Initializable {

    @FXML private CheckBox darkModeCheckBox;
    @FXML private ComboBox<String> languageCombo;
    @FXML private Label statusLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        darkModeCheckBox.setSelected(ThemeManager.isDarkMode());
        languageCombo.setItems(FXCollections.observableArrayList("English", "Hindi", "Marathi"));
        languageCombo.setValue("English");
    }

    @FXML
    private void handleApply() {
        ThemeManager.setDarkMode(darkModeCheckBox.isSelected());
        statusLabel.setText("Settings applied! Reopening page to reflect changes...");

        try {
            MainApp.switchScene("/fxml/settings.fxml");
        } catch (Exception e) {
            statusLabel.setText("Failed to apply: " + e.getMessage());
        }
    }

    @FXML
    private void goToDashboard() {
        try { MainApp.switchScene("/fxml/dashboard.fxml"); } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void goToProfile() {
        try { MainApp.switchScene("/fxml/profile.fxml"); } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void goToVehicles() {
        try { MainApp.switchScene("/fxml/vehicles.fxml"); } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void goToBookSlot() {
        try { MainApp.switchScene("/fxml/book_slot.fxml"); } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void goToMyBookings() {
        try { MainApp.switchScene("/fxml/my_bookings.fxml"); } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void goToNotifications() {
        try { MainApp.switchScene("/fxml/notifications.fxml"); } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void goToSettings() {
        try { MainApp.switchScene("/fxml/settings.fxml"); } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void handleLogout() {
        SessionManager.clearSession();
        try { MainApp.switchScene("/fxml/login.fxml"); } catch (Exception e) { e.printStackTrace(); }
    }
}
package com.parking.client.controller;

import java.net.URL;
import java.util.ResourceBundle;

import com.parking.client.MainApp;
import com.parking.client.util.SessionManager;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

public class DashboardController implements Initializable {

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label roleLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        welcomeLabel.setText("Welcome, " + SessionManager.getName() + "!");
        roleLabel.setText("Role: " + SessionManager.getRole());
    }

    @FXML
    private void handleLogout() {
        SessionManager.clearSession();
        try {
            MainApp.switchScene("/fxml/login.fxml");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goToProfile() {
        try {
            MainApp.switchScene("/fxml/profile.fxml");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goToVehicles() {
        try {
            MainApp.switchScene("/fxml/vehicles.fxml");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goToBookSlot() {
        try {
            MainApp.switchScene("/fxml/book_slot.fxml");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goToMyBookings() {
        try {
            MainApp.switchScene("/fxml/my_bookings.fxml");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
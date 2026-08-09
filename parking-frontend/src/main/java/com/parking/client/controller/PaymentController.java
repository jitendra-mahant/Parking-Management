package com.parking.client.controller;

import com.parking.client.MainApp;
import com.parking.client.util.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class PaymentController {

    @FXML private TextField cardNumberField;
    @FXML private TextField cardNameField;
    @FXML private Label statusLabel;

    @FXML
    private void handleSaveCard() {
        String cardNumber = cardNumberField.getText();
        String cardName = cardNameField.getText();

        if (cardNumber.isEmpty() || cardName.isEmpty()) {
            statusLabel.setText("Please fill in the demo card details");
            return;
        }

        // No backend call - this is a placeholder feature only
        statusLabel.setText("This is a demo - payment methods are not actually saved or processed.");
        cardNumberField.clear();
        cardNameField.clear();
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
    private void goToPayment() {
        try { MainApp.switchScene("/fxml/payment.fxml"); } catch (Exception e) { e.printStackTrace(); }
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
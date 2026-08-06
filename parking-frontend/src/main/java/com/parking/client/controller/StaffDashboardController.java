package com.parking.client.controller;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import com.parking.client.MainApp;
import com.parking.client.service.BookingApiService;
import com.parking.client.service.StaffApiService;
import com.parking.client.util.SessionManager;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

public class StaffDashboardController implements Initializable {

    @FXML private Label welcomeLabel;
    @FXML private TextField entryVehicleNumberField;
    @FXML private ComboBox<String> entryVehicleTypeCombo;
    @FXML private Label entryStatusLabel;

    @FXML private ListView<String> activeBookingsListView;
    @FXML private Label exitStatusLabel;

    @FXML private ListView<String> slotsListView;

    private List<BookingApiService.BookingData> currentActiveBookings;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        welcomeLabel.setText("Staff Dashboard - " + SessionManager.getName());
        entryVehicleTypeCombo.setItems(FXCollections.observableArrayList("Car", "Bike", "Truck"));

        // Restrict vehicle number field: uppercase, no spaces enforced by backend already
        loadActiveBookings();
        loadSlots();
    }

    @FXML
    private void handleRecordEntry() {
        String vehicleNumber = entryVehicleNumberField.getText();
        String vehicleType = entryVehicleTypeCombo.getValue();

        entryStatusLabel.setText("");

        if (vehicleNumber.isEmpty() || vehicleType == null) {
            entryStatusLabel.setText("Please enter vehicle number and select type");
            return;
        }

        new Thread(() -> {
            try {
                StaffApiService.recordEntry(vehicleNumber, vehicleType);
                Platform.runLater(() -> {
                    entryStatusLabel.setText("Entry recorded successfully");
                    entryVehicleNumberField.clear();
                    entryVehicleTypeCombo.setValue(null);
                    loadActiveBookings();
                    loadSlots();
                });
            } catch (Exception e) {
                Platform.runLater(() -> entryStatusLabel.setText("Failed: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void loadActiveBookings() {
        new Thread(() -> {
            try {
                currentActiveBookings = StaffApiService.getAllActiveBookings();
                Platform.runLater(() -> {
                    activeBookingsListView.getItems().clear();
                    for (BookingApiService.BookingData b : currentActiveBookings) {
                        activeBookingsListView.getItems().add(
                                "Slot " + b.slotNumber + " | " + b.vehicleNumber + " | Entry: " + b.entryTime
                        );
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> exitStatusLabel.setText("Failed to load bookings: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleRecordExit() {
        int selectedIndex = activeBookingsListView.getSelectionModel().getSelectedIndex();

        exitStatusLabel.setText("");

        if (selectedIndex < 0 || currentActiveBookings == null) {
            exitStatusLabel.setText("Please select a booking to exit");
            return;
        }

        Long bookingId = currentActiveBookings.get(selectedIndex).id;

        new Thread(() -> {
            try {
                BookingApiService.BookingData result = StaffApiService.recordExit(bookingId);
                Platform.runLater(() -> {
                    exitStatusLabel.setText("Exit recorded. Fare: ₹" + result.fare);
                    loadActiveBookings();
                    loadSlots();
                });
            } catch (Exception e) {
                Platform.runLater(() -> exitStatusLabel.setText("Failed: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void loadSlots() {
        new Thread(() -> {
            try {
                List<StaffApiService.SlotInfo> slots = StaffApiService.getAllSlots();
                Platform.runLater(() -> {
                    slotsListView.getItems().clear();
                    for (StaffApiService.SlotInfo slot : slots) {
                        String status = slot.occupied ? "OCCUPIED" : "FREE";
                        slotsListView.getItems().add(
                                "Slot " + slot.slotNumber + " (" + slot.vehicleType + ") - " + status
                        );
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> exitStatusLabel.setText("Failed to load slots: " + e.getMessage()));
            }
        }).start();
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
    private void goToDashboard() {
        try {
            MainApp.switchScene("/fxml/staff_dashboard.fxml");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
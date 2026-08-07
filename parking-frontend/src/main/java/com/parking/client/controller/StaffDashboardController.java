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
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

public class StaffDashboardController implements Initializable {

    @FXML private Label welcomeLabel;
    @FXML private Label totalSlotsLabel;
    @FXML private Label occupiedSlotsLabel;
    @FXML private Label availableSlotsLabel;
    @FXML private Label todayEntriesLabel;
    @FXML private Label todayExitsLabel;

    @FXML private FlowPane slotGridPane;

    @FXML private TextField entryVehicleNumberField;
    @FXML private ComboBox<String> entryVehicleTypeCombo;
    @FXML private Label entryStatusLabel;

    @FXML private ListView<String> activeBookingsListView;
    @FXML private Label exitStatusLabel;

    private List<BookingApiService.BookingData> currentActiveBookings;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        welcomeLabel.setText("Staff Dashboard - " + SessionManager.getName());
        entryVehicleTypeCombo.setItems(FXCollections.observableArrayList("Car", "Bike", "Truck"));
        refreshAll();
    }

    @FXML
    private void refreshAll() {
        loadQuickStats();
        loadSlotGrid();
        loadActiveBookings();
    }

    private void loadQuickStats() {
        new Thread(() -> {
            try {
                StaffApiService.QuickStats stats = StaffApiService.getQuickStats();
                Platform.runLater(() -> {
                    totalSlotsLabel.setText(String.valueOf(stats.totalSlots));
                    occupiedSlotsLabel.setText(String.valueOf(stats.occupiedSlots));
                    availableSlotsLabel.setText(String.valueOf(stats.availableSlots));
                    todayEntriesLabel.setText(String.valueOf(stats.todayEntries));
                    todayExitsLabel.setText(String.valueOf(stats.todayExits));
                });
            } catch (Exception e) {
                Platform.runLater(() -> exitStatusLabel.setText("Failed to load stats: " + e.getMessage()));
            }
        }).start();
    }

    private void loadSlotGrid() {
        new Thread(() -> {
            try {
                List<StaffApiService.SlotInfo> slots = StaffApiService.getAllSlots();
                Platform.runLater(() -> {
                    slotGridPane.getChildren().clear();
                    for (StaffApiService.SlotInfo slot : slots) {
                        VBox box = new VBox();
                        box.setAlignment(Pos.CENTER);
                        box.getStyleClass().add("slot-box");
                        box.getStyleClass().add(slot.occupied ? "slot-occupied" : "slot-free");

                        Label numberLabel = new Label(slot.slotNumber);
                        Label typeLabel = new Label(slot.vehicleType);
                        box.getChildren().addAll(numberLabel, typeLabel);

                        slotGridPane.getChildren().add(box);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> exitStatusLabel.setText("Failed to load slots: " + e.getMessage()));
            }
        }).start();
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
                    refreshAll();
                });
            } catch (Exception e) {
                Platform.runLater(() -> entryStatusLabel.setText("Failed: " + e.getMessage()));
            }
        }).start();
    }

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
                    refreshAll();
                });
            } catch (Exception e) {
                Platform.runLater(() -> exitStatusLabel.setText("Failed: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void goToDashboard() {
        try {
            MainApp.switchScene("/fxml/staff_dashboard.fxml");
        } catch (Exception e) {
            e.printStackTrace();
        }
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
}
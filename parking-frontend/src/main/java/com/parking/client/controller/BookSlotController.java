package com.parking.client.controller;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import com.parking.client.MainApp;
import com.parking.client.service.BookingApiService;
import com.parking.client.service.VehicleApiService;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

public class BookSlotController implements Initializable {

    @FXML private ComboBox<String> vehicleCombo;
    @FXML private ListView<String> slotListView;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;

    private List<VehicleApiService.VehicleData> myVehicles;
    private List<BookingApiService.SlotData> availableSlots;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadVehicles();
    }

    private void loadVehicles() {
        new Thread(() -> {
            try {
                myVehicles = VehicleApiService.getMyVehicles();
                Platform.runLater(() -> {
                    vehicleCombo.setItems(FXCollections.observableArrayList(
                            myVehicles.stream().map(v -> v.vehicleNumber + " (" + v.vehicleType + ")").toList()
                    ));
                });
            } catch (Exception e) {
                Platform.runLater(() -> errorLabel.setText("Failed to load vehicles: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleCheckSlots() {
        errorLabel.setText("");
        successLabel.setText("");

        int selectedIndex = vehicleCombo.getSelectionModel().getSelectedIndex();
        if (selectedIndex < 0 || myVehicles == null) {
            errorLabel.setText("Please select a vehicle first");
            return;
        }

        String vehicleType = myVehicles.get(selectedIndex).vehicleType;

        new Thread(() -> {
            try {
                availableSlots = BookingApiService.getAvailableSlots(vehicleType);
                Platform.runLater(() -> {
                    slotListView.getItems().clear();
                    if (availableSlots.isEmpty()) {
                        slotListView.getItems().add("No slots available for " + vehicleType);
                    } else {
                        for (BookingApiService.SlotData slot : availableSlots) {
                            slotListView.getItems().add("Slot " + slot.slotNumber);
                        }
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> errorLabel.setText("Failed: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleBookSlot() {
        errorLabel.setText("");
        successLabel.setText("");

        int vehicleIndex = vehicleCombo.getSelectionModel().getSelectedIndex();
        int slotIndex = slotListView.getSelectionModel().getSelectedIndex();

        if (vehicleIndex < 0 || myVehicles == null) {
            errorLabel.setText("Please select a vehicle");
            return;
        }

        if (slotIndex < 0 || availableSlots == null || availableSlots.isEmpty() || slotIndex >= availableSlots.size()) {
            errorLabel.setText("Please check and select an available slot first");
            return;
        }

        Long vehicleId = myVehicles.get(vehicleIndex).id;
        String vehicleType = myVehicles.get(vehicleIndex).vehicleType;

        new Thread(() -> {
            try {
                BookingApiService.bookSlot(vehicleId, vehicleType);
                Platform.runLater(() -> {
                    successLabel.setText("Slot booked successfully!");
                    slotListView.getItems().clear();
                });
            } catch (Exception e) {
                Platform.runLater(() -> errorLabel.setText("Booking failed: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void goToDashboard() {
        try {
            MainApp.switchScene("/fxml/dashboard.fxml");
        } catch (Exception e) {
            errorLabel.setText("Could not return to dashboard");
        }
    }
}
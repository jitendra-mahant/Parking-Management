package com.parking.client.controller;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import com.parking.client.MainApp;
import com.parking.client.service.VehicleApiService;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

public class VehicleController implements Initializable {

    @FXML private ListView<String> vehicleListView;
    @FXML private TextField vehicleNumberField;
    @FXML private ComboBox<String> vehicleTypeCombo;
    @FXML private TextField modelField;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;

    private List<VehicleApiService.VehicleData> currentVehicles;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        vehicleTypeCombo.setItems(FXCollections.observableArrayList("Car", "Bike", "Truck"));
        loadVehicles();
    }

    private void loadVehicles() {
        new Thread(() -> {
            try {
                List<VehicleApiService.VehicleData> vehicles = VehicleApiService.getMyVehicles();
                currentVehicles = vehicles;

                Platform.runLater(() -> {
                    vehicleListView.getItems().clear();
                    for (VehicleApiService.VehicleData v : vehicles) {
                        vehicleListView.getItems().add(
                                v.vehicleNumber + " - " + v.vehicleType + (v.model.isEmpty() ? "" : " (" + v.model + ")")
                        );
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> errorLabel.setText("Failed to load vehicles: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleAddVehicle() {
        String number = vehicleNumberField.getText();
        String type = vehicleTypeCombo.getValue();
        String model = modelField.getText();

        errorLabel.setText("");
        successLabel.setText("");

        if (number.isEmpty() || type == null) {
            errorLabel.setText("Vehicle number and type are required");
            return;
        }

        new Thread(() -> {
            try {
                VehicleApiService.addVehicle(number, type, model);
                Platform.runLater(() -> {
                    successLabel.setText("Vehicle added successfully");
                    vehicleNumberField.clear();
                    vehicleTypeCombo.setValue(null);
                    modelField.clear();
                    loadVehicles();
                });
            } catch (Exception e) {
                Platform.runLater(() -> errorLabel.setText("Failed: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleDeleteVehicle() {
        int selectedIndex = vehicleListView.getSelectionModel().getSelectedIndex();

        errorLabel.setText("");
        successLabel.setText("");

        if (selectedIndex < 0 || currentVehicles == null || selectedIndex >= currentVehicles.size()) {
            errorLabel.setText("Please select a vehicle to delete");
            return;
        }

        Long vehicleId = currentVehicles.get(selectedIndex).id;

        new Thread(() -> {
            try {
                VehicleApiService.deleteVehicle(vehicleId);
                Platform.runLater(() -> {
                    successLabel.setText("Vehicle deleted successfully");
                    loadVehicles();
                });
            } catch (Exception e) {
                Platform.runLater(() -> errorLabel.setText("Failed: " + e.getMessage()));
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
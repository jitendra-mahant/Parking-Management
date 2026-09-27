package com.parking.client.controller;

import com.parking.client.MainApp;
import com.parking.client.service.VehicleApiService;
import com.parking.client.util.SessionManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.util.Callback;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class VehicleController implements Initializable {

    @FXML private TableView<VehicleApiService.VehicleData> vehicleTable;
    @FXML private TableColumn<VehicleApiService.VehicleData, String> colVehicleNumber;
    @FXML private TableColumn<VehicleApiService.VehicleData, String> colVehicleType;
    @FXML private TableColumn<VehicleApiService.VehicleData, String> colModel;
    @FXML private TableColumn<VehicleApiService.VehicleData, Void> colAction;

    @FXML private TextField vehicleNumberField;
        @FXML
    private Button addVehicleButton;
    @FXML private ComboBox<String> vehicleTypeCombo;
    @FXML private TextField modelField;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        vehicleTypeCombo.setItems(FXCollections.observableArrayList("Car", "Bike", "Truck"));

        colVehicleNumber.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().vehicleNumber));
        colVehicleType.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().vehicleType));
        colModel.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().model == null || data.getValue().model.isEmpty() ? "-" : data.getValue().model));

        addDeleteButtonColumn();
        loadVehicles();
    }

    private void addDeleteButtonColumn() {
        colAction.setCellFactory(new Callback<>() {
            @Override
            public TableCell<VehicleApiService.VehicleData, Void> call(TableColumn<VehicleApiService.VehicleData, Void> param) {
                return new TableCell<>() {
                    private final Button deleteBtn = new Button("Delete");

                    {
                        deleteBtn.getStyleClass().add("table-action-btn");
                        deleteBtn.setOnAction(event -> {
                            VehicleApiService.VehicleData vehicle = getTableView().getItems().get(getIndex());
                            confirmAndDelete(vehicle);
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        setGraphic(empty ? null : deleteBtn);
                    }
                };
            }
        });
    }

    private void confirmAndDelete(VehicleApiService.VehicleData vehicle) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Vehicle");
        confirm.setHeaderText(null);
        confirm.setContentText("Remove vehicle " + vehicle.vehicleNumber + "? This cannot be undone.");
        confirm.getDialogPane().getStylesheets().add(
                MainApp.class.getResource("/css/styles.css").toExternalForm()
        );

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                new Thread(() -> {
                    try {
                        VehicleApiService.deleteVehicle(vehicle.id);
                        Platform.runLater(() -> {
                            successLabel.setText("Vehicle removed successfully");
                            errorLabel.setText("");
                            loadVehicles();
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> errorLabel.setText("Failed: " + e.getMessage()));
                    }
                }).start();
            }
        });
    }

    private void loadVehicles() {
        new Thread(() -> {
            try {
                List<VehicleApiService.VehicleData> vehicles = VehicleApiService.getMyVehicles();
                ObservableList<VehicleApiService.VehicleData> items = FXCollections.observableArrayList(vehicles);
                Platform.runLater(() -> vehicleTable.setItems(items));
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

        addVehicleButton.setDisable(true);
        addVehicleButton.setText("Adding...");

        new Thread(() -> {
            try {
                VehicleApiService.addVehicle(number, type, model);
                Platform.runLater(() -> {
                    addVehicleButton.setDisable(false);
                    addVehicleButton.setText("Add Vehicle");
                    successLabel.setText("Vehicle added successfully");
                    vehicleNumberField.clear();
                    vehicleTypeCombo.setValue(null);
                    modelField.clear();
                    loadVehicles();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    addVehicleButton.setDisable(false);
                    addVehicleButton.setText("Add Vehicle");
                    errorLabel.setText("Failed: " + e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    private void goToDashboard() {
        try { MainApp.switchScene("/fxml/dashboard.fxml"); } catch (Exception e) { errorLabel.setText("Could not navigate"); }
    }

    @FXML
    private void goToProfile() {
        try { MainApp.switchScene("/fxml/profile.fxml"); } catch (Exception e) { errorLabel.setText("Could not navigate"); }
    }

    @FXML
    private void goToVehicles() {
        try { MainApp.switchScene("/fxml/vehicles.fxml"); } catch (Exception e) { errorLabel.setText("Could not navigate"); }
    }

    @FXML
    private void goToBookSlot() {
        try { MainApp.switchScene("/fxml/book_slot.fxml"); } catch (Exception e) { errorLabel.setText("Could not navigate"); }
    }

    @FXML
    private void goToMyBookings() {
        try { MainApp.switchScene("/fxml/my_bookings.fxml"); } catch (Exception e) { errorLabel.setText("Could not navigate"); }
    }

    @FXML
    private void goToPayment() {
        try { MainApp.switchScene("/fxml/payment.fxml"); } catch (Exception e) { errorLabel.setText("Could not navigate"); }
    }

    @FXML
    private void goToNotifications() {
        try { MainApp.switchScene("/fxml/notifications.fxml"); } catch (Exception e) { errorLabel.setText("Could not navigate"); }
    }

    @FXML
    private void goToSettings() {
        try { MainApp.switchScene("/fxml/settings.fxml"); } catch (Exception e) { errorLabel.setText("Could not navigate"); }
    }

    @FXML
    private void handleLogout() {
        SessionManager.clearSession();
        try { MainApp.switchScene("/fxml/login.fxml"); } catch (Exception e) { errorLabel.setText("Could not navigate"); }
    }
}
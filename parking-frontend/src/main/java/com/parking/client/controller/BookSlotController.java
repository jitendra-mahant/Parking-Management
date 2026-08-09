package com.parking.client.controller;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import com.parking.client.MainApp;
import com.parking.client.service.BookingApiService;
import com.parking.client.service.VehicleApiService;
import com.parking.client.util.SessionManager;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

public class BookSlotController implements Initializable {

    @FXML private ComboBox<String> vehicleCombo;
    @FXML private FlowPane slotGridPane;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;

    private List<VehicleApiService.VehicleData> myVehicles;
    private List<BookingApiService.SlotData> availableSlots;
    private Long selectedSlotId;
    private VBox selectedBox;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadVehicles();
    }

    private void loadVehicles() {
        new Thread(() -> {
            try {
                myVehicles = VehicleApiService.getMyVehicles();
                Platform.runLater(() -> vehicleCombo.setItems(FXCollections.observableArrayList(
                        myVehicles.stream().map(v -> v.vehicleNumber + " (" + v.vehicleType + ")").toList()
                )));
            } catch (Exception e) {
                Platform.runLater(() -> errorLabel.setText("Failed to load vehicles: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleCheckSlots() {
        errorLabel.setText("");
        successLabel.setText("");
        selectedSlotId = null;
        selectedBox = null;

        int selectedIndex = vehicleCombo.getSelectionModel().getSelectedIndex();
        if (selectedIndex < 0 || myVehicles == null) {
            errorLabel.setText("Please select a vehicle first");
            return;
        }

        String vehicleType = myVehicles.get(selectedIndex).vehicleType;

        new Thread(() -> {
            try {
                availableSlots = BookingApiService.getAvailableSlots(vehicleType);
                Platform.runLater(this::renderSlotGrid);
            } catch (Exception e) {
                Platform.runLater(() -> errorLabel.setText("Failed: " + e.getMessage()));
            }
        }).start();
    }

    private void renderSlotGrid() {
        slotGridPane.getChildren().clear();

        if (availableSlots.isEmpty()) {
            errorLabel.setText("No slots available for this vehicle type right now");
            return;
        }

        for (BookingApiService.SlotData slot : availableSlots) {
            VBox box = new VBox();
            box.setAlignment(Pos.CENTER);
            box.getStyleClass().addAll("slot-box", "slot-free");
            box.setPrefSize(70, 60);

            Label numberLabel = new Label(slot.slotNumber);
            box.getChildren().add(numberLabel);

            box.setOnMouseClicked(e -> selectSlot(box, slot.id));

            slotGridPane.getChildren().add(box);
        }
    }

    private void selectSlot(VBox box, Long slotId) {
        if (selectedBox != null) {
            selectedBox.getStyleClass().remove("slot-selected");
        }
        box.getStyleClass().add("slot-selected");
        selectedBox = box;
        selectedSlotId = slotId;
        errorLabel.setText("");
    }

    @FXML
    private void handleBookSlot() {
        errorLabel.setText("");
        successLabel.setText("");

        int vehicleIndex = vehicleCombo.getSelectionModel().getSelectedIndex();

        if (vehicleIndex < 0 || myVehicles == null) {
            errorLabel.setText("Please select a vehicle");
            return;
        }

        if (selectedSlotId == null) {
            errorLabel.setText("Please tap a slot to select it first");
            return;
        }

        Long vehicleId = myVehicles.get(vehicleIndex).id;
        String vehicleType = myVehicles.get(vehicleIndex).vehicleType;
        String vehicleLabel = vehicleCombo.getValue();

        new Thread(() -> {
            try {
                BookingApiService.BookingData booking = BookingApiService.bookSlot(vehicleId, vehicleType);
                Platform.runLater(() -> {
                    successLabel.setText("Slot booked successfully!");
                    slotGridPane.getChildren().clear();
                    selectedSlotId = null;
                    showTicketConfirmation(booking, vehicleLabel);
                });
            } catch (Exception e) {
                Platform.runLater(() -> errorLabel.setText("Booking failed: " + e.getMessage()));
            }
        }).start();
    }

    private void showTicketConfirmation(BookingApiService.BookingData booking, String vehicleLabel) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Booking Confirmed");
        alert.setHeaderText(null);

        VBox content = new VBox(8);
        content.setAlignment(Pos.CENTER);
        content.getStyleClass().add("ticket-box");

        Label ticketIdLabel = new Label("Ticket #" + booking.id);
        ticketIdLabel.getStyleClass().add("ticket-id");

        Label slotLabel = new Label("Slot: " + booking.slotNumber);
        Label vehicleLbl = new Label("Vehicle: " + vehicleLabel);
        Label entryLabel = new Label("Entry Time: " + booking.entryTime);
        Label statusLabel = new Label("Status: " + booking.status);

        content.getChildren().addAll(ticketIdLabel, slotLabel, vehicleLbl, entryLabel, statusLabel);

        alert.getDialogPane().setContent(content);
        alert.getDialogPane().getStylesheets().add(
                MainApp.class.getResource("/css/styles.css").toExternalForm()
        );
        alert.getButtonTypes().setAll(ButtonType.OK);
        alert.showAndWait();
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
    private void goToNotifications() {
        try { MainApp.switchScene("/fxml/notifications.fxml"); } catch (Exception e) { errorLabel.setText("Could not navigate"); }
    }

    @FXML
    private void handleLogout() {
        SessionManager.clearSession();
        try { MainApp.switchScene("/fxml/login.fxml"); } catch (Exception e) { errorLabel.setText("Could not navigate"); }
    }
}
package com.parking.client.controller;

import java.io.FileWriter;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import com.parking.client.MainApp;
import com.parking.client.service.BookingApiService;
import com.parking.client.util.SessionManager;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

public class MyBookingsController implements Initializable {

    @FXML private ListView<String> activeListView;
    @FXML private ListView<String> historyListView;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;

    private List<BookingApiService.BookingData> activeBookings = new ArrayList<>();
    private List<BookingApiService.BookingData> historyBookings = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadBookings();
    }

    @FXML
    private void loadBookings() {
        errorLabel.setText("");
        successLabel.setText("");

        new Thread(() -> {
            try {
                List<BookingApiService.BookingData> all = BookingApiService.getMyBookings();

                activeBookings = all.stream()
                        .filter(b -> b.status.equals("ACTIVE"))
                        .collect(Collectors.toList());

                historyBookings = all.stream()
                        .filter(b -> !b.status.equals("ACTIVE"))
                        .collect(Collectors.toList());

                Platform.runLater(() -> {
                    activeListView.getItems().clear();
                    for (BookingApiService.BookingData b : activeBookings) {
                        activeListView.getItems().add(
                                "Slot " + b.slotNumber + " | " + b.vehicleNumber + " | Entry: " + b.entryTime
                        );
                    }

                    historyListView.getItems().clear();
                    for (BookingApiService.BookingData b : historyBookings) {
                        String fareText = b.fare != null ? " | ₹" + b.fare : "";
                        historyListView.getItems().add(
                                "Slot " + b.slotNumber + " | " + b.vehicleNumber + " | " + b.status + fareText
                        );
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> errorLabel.setText("Failed to load bookings: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleCancel() {
        errorLabel.setText("");
        successLabel.setText("");

        int selectedIndex = activeListView.getSelectionModel().getSelectedIndex();
        if (selectedIndex < 0 || activeBookings.isEmpty()) {
            errorLabel.setText("Please select an active booking");
            return;
        }

        Long bookingId = activeBookings.get(selectedIndex).id;

        new Thread(() -> {
            try {
                BookingApiService.cancelBooking(bookingId);
                Platform.runLater(() -> {
                    successLabel.setText("Booking cancelled");
                    loadBookings();
                });
            } catch (Exception e) {
                Platform.runLater(() -> errorLabel.setText("Failed: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleComplete() {
        errorLabel.setText("");
        successLabel.setText("");

        int selectedIndex = activeListView.getSelectionModel().getSelectedIndex();
        if (selectedIndex < 0 || activeBookings.isEmpty()) {
            errorLabel.setText("Please select an active booking");
            return;
        }

        Long bookingId = activeBookings.get(selectedIndex).id;

        new Thread(() -> {
            try {
                BookingApiService.completeBooking(bookingId);
                Platform.runLater(() -> {
                    successLabel.setText("Booking completed! Fare calculated.");
                    loadBookings();
                });
            } catch (Exception e) {
                Platform.runLater(() -> errorLabel.setText("Failed: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleDownloadReceipt() {
        errorLabel.setText("");
        successLabel.setText("");

        int selectedIndex = historyListView.getSelectionModel().getSelectedIndex();
        if (selectedIndex < 0 || historyBookings.isEmpty()) {
            errorLabel.setText("Please select a booking from History");
            return;
        }

        BookingApiService.BookingData b = historyBookings.get(selectedIndex);

        try {
            String fileName = "Receipt_Booking_" + b.id + ".txt";
            String userHome = System.getProperty("user.home");
            String filePath = Paths.get(userHome, "Downloads", fileName).toString();

            String content = "PARKING MANAGEMENT SYSTEM - RECEIPT\n" +
                    "=====================================\n" +
                    "Booking ID: " + b.id + "\n" +
                    "Customer: " + SessionManager.getName() + "\n" +
                    "Vehicle: " + b.vehicleNumber + "\n" +
                    "Slot: " + b.slotNumber + "\n" +
                    "Entry Time: " + b.entryTime + "\n" +
                    "Exit Time: " + (b.exitTime != null ? b.exitTime : "N/A") + "\n" +
                    "Status: " + b.status + "\n" +
                    "Fare: " + (b.fare != null ? "₹" + b.fare : "N/A") + "\n" +
                    "=====================================\n" +
                    "Thank you for using our parking service!";

            try (FileWriter writer = new FileWriter(filePath)) {
                writer.write(content);
            }

            successLabel.setText("Receipt saved to Downloads: " + fileName);
        } catch (Exception e) {
            errorLabel.setText("Failed to save receipt: " + e.getMessage());
        }
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

    @FXML
    private void goToPayment() {
        try { MainApp.switchScene("/fxml/payment.fxml"); } catch (Exception e) { errorLabel.setText("Could not navigate"); }
    }

    @FXML
    private void goToSettings() {
        try { MainApp.switchScene("/fxml/settings.fxml"); } catch (Exception e) { errorLabel.setText("Could not navigate"); }
    }
}
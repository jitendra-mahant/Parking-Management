package com.parking.client.controller;

import com.parking.client.MainApp;
import com.parking.client.service.BookingApiService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class MyBookingsController implements Initializable {

    @FXML private ListView<String> bookingListView;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;

    private List<BookingApiService.BookingData> currentBookings;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadBookings();
    }

    @FXML
    private void loadBookings() {
        new Thread(() -> {
            try {
                currentBookings = BookingApiService.getMyBookings();
                Platform.runLater(() -> {
                    bookingListView.getItems().clear();
                    for (BookingApiService.BookingData b : currentBookings) {
                        String fareText = b.fare != null ? " | Fare: ₹" + b.fare : "";
                        bookingListView.getItems().add(
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

        int selectedIndex = bookingListView.getSelectionModel().getSelectedIndex();
        if (selectedIndex < 0 || currentBookings == null) {
            errorLabel.setText("Please select a booking");
            return;
        }

        Long bookingId = currentBookings.get(selectedIndex).id;

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

        int selectedIndex = bookingListView.getSelectionModel().getSelectedIndex();
        if (selectedIndex < 0 || currentBookings == null) {
            errorLabel.setText("Please select a booking");
            return;
        }

        Long bookingId = currentBookings.get(selectedIndex).id;

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
    private void goToDashboard() {
        try {
            MainApp.switchScene("/fxml/dashboard.fxml");
        } catch (Exception e) {
            errorLabel.setText("Could not return to dashboard");
        }
    }
}
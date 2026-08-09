package com.parking.client.controller;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import com.parking.client.MainApp;
import com.parking.client.service.AdminApiService;
import com.parking.client.service.BookingApiService;
import com.parking.client.util.SessionManager;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class AdminDashboardController implements Initializable {

    @FXML private Label welcomeLabel;
    @FXML private Label revenueLabel;

    @FXML private TextField staffNameField;
    @FXML private TextField staffEmailField;
    @FXML private PasswordField staffPasswordField;
    @FXML private TextField staffPhoneField;
    @FXML private Label staffStatusLabel;
    @FXML private ListView<String> staffListView;

    @FXML private ListView<String> bookingsListView;

    private List<AdminApiService.StaffData> currentStaff;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        welcomeLabel.setText("Admin Dashboard - " + SessionManager.getName());
        loadRevenue();
        loadStaff();
        loadBookings();
    }

    @FXML
    private void loadRevenue() {
        new Thread(() -> {
            try {
                AdminApiService.RevenueData data = AdminApiService.getRevenueReport();
                Platform.runLater(() -> revenueLabel.setText(
                        "Total: " + data.totalBookings + " | Active: " + data.activeBookings +
                        " | Completed: " + data.completedBookings + " | Revenue: ₹" + data.totalRevenue
                ));
            } catch (Exception e) {
                Platform.runLater(() -> revenueLabel.setText("Failed to load revenue: " + e.getMessage()));
            }
        }).start();
    }

    private void loadStaff() {
        new Thread(() -> {
            try {
                currentStaff = AdminApiService.getAllStaff();
                Platform.runLater(() -> {
                    staffListView.getItems().clear();
                    for (AdminApiService.StaffData s : currentStaff) {
                        staffListView.getItems().add(s.name + " - " + s.email + " - " + s.phone);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> staffStatusLabel.setText("Failed to load staff: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleAddStaff() {
        String name = staffNameField.getText();
        String email = staffEmailField.getText();
        String password = staffPasswordField.getText();
        String phone = staffPhoneField.getText();

        staffStatusLabel.setText("");

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            staffStatusLabel.setText("Name, email, and password are required");
            return;
        }

        new Thread(() -> {
            try {
                AdminApiService.createStaff(name, email, password, phone);
                Platform.runLater(() -> {
                    staffStatusLabel.setText("Staff added successfully");
                    staffNameField.clear();
                    staffEmailField.clear();
                    staffPasswordField.clear();
                    staffPhoneField.clear();
                    loadStaff();
                });
            } catch (Exception e) {
                Platform.runLater(() -> staffStatusLabel.setText("Failed: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleRemoveStaff() {
        int selectedIndex = staffListView.getSelectionModel().getSelectedIndex();

        staffStatusLabel.setText("");

        if (selectedIndex < 0 || currentStaff == null) {
            staffStatusLabel.setText("Please select a staff member to remove");
            return;
        }

        Long staffId = currentStaff.get(selectedIndex).id;

        new Thread(() -> {
            try {
                AdminApiService.deleteStaff(staffId);
                Platform.runLater(() -> {
                    staffStatusLabel.setText("Staff removed");
                    loadStaff();
                });
            } catch (Exception e) {
                Platform.runLater(() -> staffStatusLabel.setText("Failed: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void loadBookings() {
        new Thread(() -> {
            try {
                List<BookingApiService.BookingData> bookings = AdminApiService.getAllBookings();
                Platform.runLater(() -> {
                    bookingsListView.getItems().clear();
                    for (BookingApiService.BookingData b : bookings) {
                        String fareText = b.fare != null ? " | ₹" + b.fare : "";
                        bookingsListView.getItems().add(
                                "Slot " + b.slotNumber + " | " + b.vehicleNumber + " | " + b.status + fareText
                        );
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> staffStatusLabel.setText("Failed to load bookings: " + e.getMessage()));
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
            MainApp.switchScene("/fxml/admin_dashboard.fxml");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
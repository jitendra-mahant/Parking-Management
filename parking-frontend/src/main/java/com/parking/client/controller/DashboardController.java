package com.parking.client.controller;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import com.parking.client.MainApp;
import com.parking.client.service.BookingApiService;
import com.parking.client.service.VehicleApiService;
import com.parking.client.util.SessionManager;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class DashboardController implements Initializable {

    @FXML private Label welcomeLabel;
    @FXML private Label roleLabel;
    @FXML private Label avatarLabel;
    @FXML private Label sidebarUserName;
    @FXML private Label sidebarUserRole;
    @FXML private Label activeBookingsLabel;
    @FXML private Label totalBookingsLabel;
    @FXML private Label totalSpentLabel;
    @FXML private Label vehiclesCountLabel;

    @FXML private TableView<BookingApiService.BookingData> recentBookingsTable;
    @FXML private TableColumn<BookingApiService.BookingData, String> colRecentSlot;
    @FXML private TableColumn<BookingApiService.BookingData, String> colRecentVehicle;
    @FXML private TableColumn<BookingApiService.BookingData, String> colRecentCheckIn;
    @FXML private TableColumn<BookingApiService.BookingData, String> colRecentCheckOut;
    @FXML private TableColumn<BookingApiService.BookingData, Void> colRecentStatus;
    @FXML private TableColumn<BookingApiService.BookingData, String> colRecentFare;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        String name = SessionManager.getName();
        String role = SessionManager.getRole();
        if (name == null || name.isEmpty()) name = "User";
        if (role == null || role.isEmpty()) role = "customer";

        welcomeLabel.setText("Welcome back, " + name + "!");
        roleLabel.setText(role.toUpperCase());
        if (sidebarUserName != null) sidebarUserName.setText(name);
        if (sidebarUserRole != null) sidebarUserRole.setText(role.toUpperCase());
        avatarLabel.setText(name.substring(0, 1).toUpperCase());

        setupRecentTable();
        loadStats();
        loadVehiclesCount();
        loadRecentBookings();
    }

    private void setupRecentTable() {
        if (recentBookingsTable == null) return;

        colRecentSlot.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().slotNumber));
        colRecentVehicle.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().vehicleNumber));
        colRecentCheckIn.setCellValueFactory(d -> new SimpleStringProperty(formatDateTime(d.getValue().entryTime)));
        colRecentCheckOut.setCellValueFactory(d -> new SimpleStringProperty(formatDateTime(d.getValue().expectedExitTime)));
        colRecentFare.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().fare != null ? "₹" + d.getValue().fare :
                (d.getValue().estimatedFare != null ? "₹" + String.format("%.0f", d.getValue().estimatedFare) : "-")));

        colRecentStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }
                BookingApiService.BookingData b = getTableView().getItems().get(getIndex());
                Label badge = new Label(b.status != null ? b.status : "ACTIVE");
                if ("COMPLETED".equalsIgnoreCase(b.status)) {
                    badge.getStyleClass().add("status-badge-completed");
                } else if ("CANCELLED".equalsIgnoreCase(b.status)) {
                    badge.getStyleClass().add("status-badge-overstay");
                } else if (b.upcoming) {
                    badge.setText("Upcoming");
                    badge.getStyleClass().add("status-badge-upcoming");
                } else {
                    badge.setText("Active");
                    badge.getStyleClass().add("status-badge-ontime");
                }
                setGraphic(badge);
            }
        });
    }

    private String formatDateTime(String iso) {
        if (iso == null) return "-";
        String cleaned = iso.replace("T", " ");
        return cleaned.length() >= 16 ? cleaned.substring(0, 16) : cleaned;
    }

    private void loadStats() {
        new Thread(() -> {
            try {
                BookingApiService.CustomerStats stats = BookingApiService.getMyStats();
                Platform.runLater(() -> {
                    activeBookingsLabel.setText(String.valueOf(stats.activeBookings));
                    totalBookingsLabel.setText(String.valueOf(stats.totalBookings));
                    totalSpentLabel.setText("₹" + stats.totalSpent);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    activeBookingsLabel.setText("0");
                    totalBookingsLabel.setText("0");
                    totalSpentLabel.setText("₹0");
                });
            }
        }).start();
    }

    private void loadVehiclesCount() {
        new Thread(() -> {
            try {
                var vehicles = VehicleApiService.getMyVehicles();
                Platform.runLater(() -> {
                    if (vehiclesCountLabel != null) {
                        vehiclesCountLabel.setText(String.valueOf(vehicles.size()));
                    }
                });
            } catch (Exception e) {
                if (vehiclesCountLabel != null) {
                    Platform.runLater(() -> vehiclesCountLabel.setText("0"));
                }
            }
        }).start();
    }

    private void loadRecentBookings() {
        if (recentBookingsTable == null) return;
        new Thread(() -> {
            try {
                List<BookingApiService.BookingData> all = BookingApiService.getMyBookings();
                // Take latest 5 bookings
                List<BookingApiService.BookingData> recent = all.stream().limit(5).collect(Collectors.toList());
                Platform.runLater(() -> recentBookingsTable.setItems(FXCollections.observableArrayList(recent)));
            } catch (Exception e) {
                // Table placeholder displays error or empty
            }
        }).start();
    }

    @FXML
    private void handleLogout() {
        SessionManager.clearSession();
        try { MainApp.switchScene("/fxml/login.fxml"); } catch (Exception e) { e.printStackTrace(); }
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
}

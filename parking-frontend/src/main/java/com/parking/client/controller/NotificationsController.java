package com.parking.client.controller;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import com.parking.client.MainApp;
import com.parking.client.service.NotificationApiService;
import com.parking.client.util.SessionManager;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

public class NotificationsController implements Initializable {

    @FXML private ListView<String> notificationsListView;
    @FXML private Label statusLabel;

    private List<NotificationApiService.NotificationData> currentNotifications;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadNotifications();
    }

    @FXML
    private void loadNotifications() {
        new Thread(() -> {
            try {
                currentNotifications = NotificationApiService.getMyNotifications();
                Platform.runLater(() -> {
                    notificationsListView.getItems().clear();
                    for (NotificationApiService.NotificationData n : currentNotifications) {
                        String prefix = n.isRead ? "[Read] " : "[New] ";
                        notificationsListView.getItems().add(prefix + n.message + " (" + n.createdAt + ")");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Failed to load: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleMarkAsRead() {
        int selectedIndex = notificationsListView.getSelectionModel().getSelectedIndex();

        statusLabel.setText("");

        if (selectedIndex < 0 || currentNotifications == null) {
            statusLabel.setText("Please select a notification");
            return;
        }

        Long id = currentNotifications.get(selectedIndex).id;

        new Thread(() -> {
            try {
                NotificationApiService.markAsRead(id);
                Platform.runLater(() -> {
                    statusLabel.setText("Marked as read");
                    loadNotifications();
                });
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Failed: " + e.getMessage()));
            }
        }).start();
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
    private void goToNotifications() {
        try { MainApp.switchScene("/fxml/notifications.fxml"); } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void handleLogout() {
        SessionManager.clearSession();
        try { MainApp.switchScene("/fxml/login.fxml"); } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void goToPayment() {
        try { MainApp.switchScene("/fxml/payment.fxml"); } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void goToSettings() {
        try { MainApp.switchScene("/fxml/settings.fxml"); } catch (Exception e) { e.printStackTrace(); }
    }
}
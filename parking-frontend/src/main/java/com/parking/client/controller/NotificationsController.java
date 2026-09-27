package com.parking.client.controller;

import com.parking.client.MainApp;
import com.parking.client.service.NotificationApiService;
import com.parking.client.util.SessionManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.util.Callback;

import java.net.URL;
import java.util.ResourceBundle;

public class NotificationsController implements Initializable {

    @FXML private TableView<NotificationApiService.NotificationData> notifTable;
    @FXML private TableColumn<NotificationApiService.NotificationData, String> colStatus;
    @FXML private TableColumn<NotificationApiService.NotificationData, String> colMessage;
    @FXML private TableColumn<NotificationApiService.NotificationData, String> colTime;
    @FXML private TableColumn<NotificationApiService.NotificationData, Void> colNotifAction;

    @FXML private Label statusLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                NotificationApiService.NotificationData n =
                        (NotificationApiService.NotificationData) getTableRow().getItem();
                if (!n.isRead) {
                    Region dot = new Region();
                    dot.getStyleClass().add("notif-dot");
                    setGraphic(dot);
                } else {
                    setGraphic(null);
                }
            }
        });

        colMessage.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().message));
        colTime.setCellValueFactory(d -> new SimpleStringProperty(formatDateTime(d.getValue().createdAt)));

        colNotifAction.setCellFactory(new Callback<>() {
            @Override
            public TableCell<NotificationApiService.NotificationData, Void> call(TableColumn<NotificationApiService.NotificationData, Void> param) {
                return new TableCell<>() {
                    private final Button markBtn = new Button("Mark Read");

                    {
                        markBtn.setStyle("-fx-font-size: 11px; -fx-padding: 5 12 5 12; -fx-min-width: 0; " +
                                "-fx-background-color: #eef1f8; -fx-text-fill: #4361ee; -fx-effect: none;");
                        markBtn.setOnAction(e -> {
                            NotificationApiService.NotificationData n = getTableView().getItems().get(getIndex());
                            markAsRead(n);
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                            return;
                        }
                        NotificationApiService.NotificationData n = getTableView().getItems().get(getIndex());
                        setGraphic(n.isRead ? null : markBtn);
                    }
                };
            }
        });

        notifTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(NotificationApiService.NotificationData item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().remove("notif-row-unread");
                if (item != null && !empty && !item.isRead) {
                    getStyleClass().add("notif-row-unread");
                }
            }
        });

        loadNotifications();
    }

    private String formatDateTime(String iso) {
        if (iso == null) return "-";
        return iso.replace("T", " ").length() >= 16 ? iso.replace("T", " ").substring(0, 16) : iso;
    }

    @FXML
    private void loadNotifications() {
        statusLabel.setText("");
        new Thread(() -> {
            try {
                var notifications = NotificationApiService.getMyNotifications();
                Platform.runLater(() -> notifTable.setItems(FXCollections.observableArrayList(notifications)));
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Failed to load: " + e.getMessage()));
            }
        }).start();
    }

    private void markAsRead(NotificationApiService.NotificationData n) {
        new Thread(() -> {
            try {
                NotificationApiService.markAsRead(n.id);
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

    @FXML
    private void handleLogout() {
        SessionManager.clearSession();
        try { MainApp.switchScene("/fxml/login.fxml"); } catch (Exception e) { e.printStackTrace(); }
    }
}   
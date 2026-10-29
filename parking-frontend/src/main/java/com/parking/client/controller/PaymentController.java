package com.parking.client.controller;

import com.parking.client.MainApp;
import com.parking.client.service.BookingApiService;
import com.parking.client.util.SessionManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class PaymentController implements Initializable {

    @FXML private TableView<BookingApiService.BookingData> transactionTable;
    @FXML private TableColumn<BookingApiService.BookingData, String> colTxnId;
    @FXML private TableColumn<BookingApiService.BookingData, String> colTxnDate;
    @FXML private TableColumn<BookingApiService.BookingData, String> colTxnVehicle;
    @FXML private TableColumn<BookingApiService.BookingData, String> colTxnSlot;
    @FXML private TableColumn<BookingApiService.BookingData, String> colTxnAmount;
    @FXML private TableColumn<BookingApiService.BookingData, Void> colTxnStatus;

    @FXML private Label totalPaidLabel;
    @FXML private Label txnCountLabel;

    @FXML private ListView<String> savedCardsList;
    @FXML private TextField cardNumberField;
    @FXML private TextField cardNameField;
    @FXML private Label statusLabel;

    private static final ObservableList<String> staticSavedCards = FXCollections.observableArrayList(
            "•••• •••• •••• 4242 — Visa (Default)",
            "•••• •••• •••• 8821 — Mastercard"
    );

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (savedCardsList != null) {
            savedCardsList.setItems(staticSavedCards);
        }

        if (transactionTable != null) {
            colTxnId.setCellValueFactory(d -> new SimpleStringProperty("TXN-" + d.getValue().id + "89"));
            colTxnDate.setCellValueFactory(d -> new SimpleStringProperty(formatDateTime(
                    d.getValue().exitTime != null ? d.getValue().exitTime : d.getValue().entryTime)));
            colTxnVehicle.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().vehicleNumber));
            colTxnSlot.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().slotNumber));
            colTxnAmount.setCellValueFactory(d -> new SimpleStringProperty(
                    d.getValue().fare != null ? "₹" + String.format("%.2f", d.getValue().fare) : "₹0.00"));

            colTxnStatus.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                        setGraphic(null);
                        return;
                    }
                    Label badge = new Label("PAID");
                    badge.getStyleClass().add("status-badge-ontime");
                    setGraphic(badge);
                }
            });

            loadTransactions();
        }
    }

    private String formatDateTime(String iso) {
        if (iso == null) return "-";
        String cleaned = iso.replace("T", " ");
        return cleaned.length() >= 16 ? cleaned.substring(0, 16) : cleaned;
    }

    private void loadTransactions() {
        new Thread(() -> {
            try {
                List<BookingApiService.BookingData> all = BookingApiService.getMyBookings();
                // Real transactions are completed bookings with fare
                List<BookingApiService.BookingData> paid = all.stream()
                        .filter(b -> "COMPLETED".equalsIgnoreCase(b.status) && b.fare != null)
                        .collect(Collectors.toList());

                double totalSpent = paid.stream().mapToDouble(b -> b.fare).sum();

                Platform.runLater(() -> {
                    transactionTable.setItems(FXCollections.observableArrayList(paid));
                    if (totalPaidLabel != null) totalPaidLabel.setText("₹" + String.format("%.2f", totalSpent));
                    if (txnCountLabel != null) txnCountLabel.setText(String.valueOf(paid.size()));
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (statusLabel != null) statusLabel.setText("Failed to load transactions: " + e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    private void handleSaveCard() {
        String cardNumber = cardNumberField.getText() != null ? cardNumberField.getText().trim() : "";
        String cardName = cardNameField.getText() != null ? cardNameField.getText().trim() : "";

        if (cardNumber.isEmpty() || cardName.isEmpty()) {
            statusLabel.setStyle("-fx-text-fill: #D64545; -fx-font-weight: bold;");
            statusLabel.setText("Please enter card / UPI details and name.");
            return;
        }

        String last4 = cardNumber.length() >= 4 ? cardNumber.substring(cardNumber.length() - 4) : cardNumber;
        String display = "•••• •••• •••• " + last4 + " — " + cardName;
        staticSavedCards.add(display);

        statusLabel.setStyle("-fx-text-fill: #21865B; -fx-font-weight: bold;");
        statusLabel.setText("Payment method saved to your profile.");
        cardNumberField.clear();
        cardNameField.clear();
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

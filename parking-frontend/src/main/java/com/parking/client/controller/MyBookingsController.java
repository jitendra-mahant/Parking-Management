package com.parking.client.controller;

import com.parking.client.MainApp;
import com.parking.client.service.BookingApiService;
import com.parking.client.util.SessionManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.collections.transformation.FilteredList;
import javafx.util.Callback;

import java.io.FileWriter;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class MyBookingsController implements Initializable {

    @FXML
    private TableView<BookingApiService.BookingData> activeTable;
    @FXML
    private TableColumn<BookingApiService.BookingData, String> colActiveSlot;
    @FXML
    private TableColumn<BookingApiService.BookingData, String> colActiveVehicle;
    @FXML
    private TableColumn<BookingApiService.BookingData, String> colActiveEntry;
    @FXML
    private TableColumn<BookingApiService.BookingData, Void> colActiveAction;

    @FXML
    private TableView<BookingApiService.BookingData> historyTable;
    @FXML
    private TableColumn<BookingApiService.BookingData, String> colHistSlot;
    @FXML
    private TableColumn<BookingApiService.BookingData, String> colHistVehicle;
    @FXML
    private TableColumn<BookingApiService.BookingData, String> colHistStatus;
    @FXML
    private TableColumn<BookingApiService.BookingData, String> colHistFare;
    @FXML
    private TableColumn<BookingApiService.BookingData, Void> colHistAction;

    @FXML
    private Label errorLabel;
    @FXML
    private Label successLabel;

    @FXML
    private TextField historySearchField;

    private final javafx.collections.ObservableList<BookingApiService.BookingData> allHistory
            = javafx.collections.FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colActiveSlot.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().slotNumber));
        colActiveVehicle.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().vehicleNumber));
        colActiveEntry.setCellValueFactory(d -> new SimpleStringProperty(formatDateTime(d.getValue().entryTime)));
        addActiveActionButtons();

        colHistSlot.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().slotNumber));
        colHistVehicle.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().vehicleNumber));
        colHistStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().status));
        colHistFare.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().fare != null ? "₹" + d.getValue().fare : "-"));
        addHistoryActionButton();

        loadBookings();
        FilteredList<BookingApiService.BookingData> filteredHistory = new FilteredList<>(allHistory, b -> true);
        historyTable.setItems(filteredHistory);

        historySearchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String query = newVal == null ? "" : newVal.trim().toLowerCase();
            filteredHistory.setPredicate(b -> query.isEmpty() || b.vehicleNumber.toLowerCase().contains(query));
        });
    }

    private String formatDateTime(String iso) {
        if (iso == null) {
            return "-";
        }
        return iso.replace("T", " ").length() >= 16 ? iso.replace("T", " ").substring(0, 16) : iso;
    }

    private void addActiveActionButtons() {
        colActiveAction.setCellFactory(new Callback<>() {
            @Override
            public TableCell<BookingApiService.BookingData, Void> call(TableColumn<BookingApiService.BookingData, Void> param) {
                return new TableCell<>() {
                    private final Button completeBtn = new Button("Complete");
                    private final Button cancelBtn = new Button("Cancel");
                    private final HBox box = new HBox(6, completeBtn, cancelBtn);

                    {
                        completeBtn.getStyleClass().add("customer-btn");
                        completeBtn.setStyle("-fx-font-size: 11px; -fx-padding: 5 12 5 12; -fx-min-width: 0;");
                        cancelBtn.getStyleClass().add("table-action-btn");

                        completeBtn.setOnAction(e -> {
                            BookingApiService.BookingData b = getTableView().getItems().get(getIndex());
                            handleComplete(b);
                        });
                        cancelBtn.setOnAction(e -> {
                            BookingApiService.BookingData b = getTableView().getItems().get(getIndex());
                            confirmAndCancel(b);
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        setGraphic(empty ? null : box);
                    }
                };
            }
        });
    }

    private void addHistoryActionButton() {
        colHistAction.setCellFactory(new Callback<>() {
            @Override
            public TableCell<BookingApiService.BookingData, Void> call(TableColumn<BookingApiService.BookingData, Void> param) {
                return new TableCell<>() {
                    private final Button receiptBtn = new Button("Download");

                    {
                        receiptBtn.setStyle("-fx-font-size: 11px; -fx-padding: 5 12 5 12; -fx-min-width: 0; "
                                + "-fx-background-color: #eef1f8; -fx-text-fill: #4361ee; -fx-effect: none;");
                        receiptBtn.setOnAction(e -> {
                            BookingApiService.BookingData b = getTableView().getItems().get(getIndex());
                            downloadReceipt(b);
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        setGraphic(empty ? null : receiptBtn);
                    }
                };
            }
        });
    }

    @FXML
    private void loadBookings() {
        errorLabel.setText("");
        successLabel.setText("");

        new Thread(() -> {
            try {
                List<BookingApiService.BookingData> all = BookingApiService.getMyBookings();

                List<BookingApiService.BookingData> active = all.stream()
                        .filter(b -> b.status.equals("ACTIVE")).collect(Collectors.toList());
                List<BookingApiService.BookingData> history = all.stream()
                        .filter(b -> !b.status.equals("ACTIVE")).collect(Collectors.toList());

                Platform.runLater(() -> {
                    activeTable.setItems(FXCollections.observableArrayList(active));
                    allHistory.setAll(history);
                });
            } catch (Exception e) {
                Platform.runLater(() -> errorLabel.setText("Failed to load bookings: " + e.getMessage()));
            }
        }).start();
    }

    private void handleComplete(BookingApiService.BookingData booking) {
        errorLabel.setText("");
        successLabel.setText("");

        new Thread(() -> {
            try {
                BookingApiService.completeBooking(booking.id);
                Platform.runLater(() -> {
                    successLabel.setText("Booking completed! Fare calculated.");
                    loadBookings();
                });
            } catch (Exception e) {
                Platform.runLater(() -> errorLabel.setText("Failed: " + e.getMessage()));
            }
        }).start();
    }

    private void confirmAndCancel(BookingApiService.BookingData booking) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Cancel Booking");
        confirm.setHeaderText(null);
        confirm.setContentText("Cancel booking for slot " + booking.slotNumber + "?");
        confirm.getDialogPane().getStylesheets().add(
                MainApp.class.getResource("/css/styles.css").toExternalForm()
        );

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                new Thread(() -> {
                    try {
                        BookingApiService.cancelBooking(booking.id);
                        Platform.runLater(() -> {
                            successLabel.setText("Booking cancelled");
                            errorLabel.setText("");
                            loadBookings();
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> errorLabel.setText("Failed: " + e.getMessage()));
                    }
                }).start();
            }
        });
    }

    private void downloadReceipt(BookingApiService.BookingData b) {
        errorLabel.setText("");
        successLabel.setText("");

        try {
            String fileName = "Receipt_Booking_" + b.id + ".txt";
            String userHome = System.getProperty("user.home");
            String filePath = Paths.get(userHome, "Downloads", fileName).toString();

            String content = "PARKING MANAGEMENT SYSTEM - RECEIPT\n"
                    + "=====================================\n"
                    + "Booking ID: " + b.id + "\n"
                    + "Customer: " + SessionManager.getName() + "\n"
                    + "Vehicle: " + b.vehicleNumber + "\n"
                    + "Slot: " + b.slotNumber + "\n"
                    + "Entry Time: " + b.entryTime + "\n"
                    + "Exit Time: " + (b.exitTime != null ? b.exitTime : "N/A") + "\n"
                    + "Status: " + b.status + "\n"
                    + "Fare: " + (b.fare != null ? "₹" + b.fare : "N/A") + "\n"
                    + "=====================================\n"
                    + "Thank you for using our parking service!";

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
        try {
            MainApp.switchScene("/fxml/dashboard.fxml");
        } catch (Exception e) {
            errorLabel.setText("Could not navigate");
        }
    }

    @FXML
    private void goToProfile() {
        try {
            MainApp.switchScene("/fxml/profile.fxml");
        } catch (Exception e) {
            errorLabel.setText("Could not navigate");
        }
    }

    @FXML
    private void goToVehicles() {
        try {
            MainApp.switchScene("/fxml/vehicles.fxml");
        } catch (Exception e) {
            errorLabel.setText("Could not navigate");
        }
    }

    @FXML
    private void goToBookSlot() {
        try {
            MainApp.switchScene("/fxml/book_slot.fxml");
        } catch (Exception e) {
            errorLabel.setText("Could not navigate");
        }
    }

    @FXML
    private void goToMyBookings() {
        try {
            MainApp.switchScene("/fxml/my_bookings.fxml");
        } catch (Exception e) {
            errorLabel.setText("Could not navigate");
        }
    }

    @FXML
    private void goToPayment() {
        try {
            MainApp.switchScene("/fxml/payment.fxml");
        } catch (Exception e) {
            errorLabel.setText("Could not navigate");
        }
    }

    @FXML
    private void goToNotifications() {
        try {
            MainApp.switchScene("/fxml/notifications.fxml");
        } catch (Exception e) {
            errorLabel.setText("Could not navigate");
        }
    }

    @FXML
    private void goToSettings() {
        try {
            MainApp.switchScene("/fxml/settings.fxml");
        } catch (Exception e) {
            errorLabel.setText("Could not navigate");
        }
    }

    @FXML
    private void handleLogout() {
        SessionManager.clearSession();
        try {
            MainApp.switchScene("/fxml/login.fxml");
        } catch (Exception e) {
            errorLabel.setText("Could not navigate");
        }
    }
}

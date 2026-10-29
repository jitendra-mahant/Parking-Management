package com.parking.client.controller;

import com.parking.client.MainApp;
import com.parking.client.service.BookingApiService;
import com.parking.client.util.SessionManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import java.io.FileWriter;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class MyBookingsController implements Initializable {

    @FXML private TableView<BookingApiService.BookingData> activeTable;
    @FXML private TableColumn<BookingApiService.BookingData, String> colActiveSlot;
    @FXML private TableColumn<BookingApiService.BookingData, String> colActiveVehicle;
    @FXML private TableColumn<BookingApiService.BookingData, String> colActiveCheckIn;
    @FXML private TableColumn<BookingApiService.BookingData, String> colActiveCheckOut;
    @FXML private TableColumn<BookingApiService.BookingData, Void> colActiveStatus;
    @FXML private TableColumn<BookingApiService.BookingData, String> colActiveFare;
    @FXML private TableColumn<BookingApiService.BookingData, Void> colActiveAction;

    @FXML private TextField historySearchField;
    @FXML private TableView<BookingApiService.BookingData> historyTable;
    @FXML private TableColumn<BookingApiService.BookingData, String> colHistSlot;
    @FXML private TableColumn<BookingApiService.BookingData, String> colHistVehicle;
    @FXML private TableColumn<BookingApiService.BookingData, String> colHistStatus;
    @FXML private TableColumn<BookingApiService.BookingData, String> colHistFare;
    @FXML private TableColumn<BookingApiService.BookingData, String> colHistRating;
    @FXML private TableColumn<BookingApiService.BookingData, Void> colHistAction;

    @FXML private Label errorLabel;
    @FXML private Label successLabel;

    private final ObservableList<BookingApiService.BookingData> allHistory = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colActiveSlot.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().slotNumber));
        colActiveVehicle.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().vehicleNumber));
        colActiveCheckIn.setCellValueFactory(d -> new SimpleStringProperty(formatDateTime(d.getValue().entryTime)));
        colActiveCheckOut.setCellValueFactory(d -> new SimpleStringProperty(formatDateTime(d.getValue().expectedExitTime)));
        colActiveFare.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().estimatedFare != null ? "₹" + String.format("%.0f", d.getValue().estimatedFare) : "-"));
        addActiveStatusColumn();
        addActiveActionButtons();

        colHistSlot.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().slotNumber));
        colHistVehicle.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().vehicleNumber));
        colHistStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().status));
        colHistFare.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().fare != null ? "₹" + d.getValue().fare : "-"));
        colHistRating.setCellValueFactory(d -> new SimpleStringProperty(formatStars(d.getValue().rating)));
        addHistoryActionButton();

        FilteredList<BookingApiService.BookingData> filteredHistory = new FilteredList<>(allHistory, b -> true);
        historyTable.setItems(filteredHistory);

        historySearchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String query = newVal == null ? "" : newVal.trim().toLowerCase();
            filteredHistory.setPredicate(b -> query.isEmpty() || b.vehicleNumber.toLowerCase().contains(query));
        });

        loadBookings();
    }

    private String formatDateTime(String iso) {
        if (iso == null) return "-";
        String cleaned = iso.replace("T", " ");
        return cleaned.length() >= 16 ? cleaned.substring(0, 16) : cleaned;
    }

    private String formatStars(Integer rating) {
        if (rating == null) return "-";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            sb.append(i < rating ? "★" : "☆");
        }
        return sb.toString();
    }

    private void addActiveStatusColumn() {
        colActiveStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }
                BookingApiService.BookingData b = getTableView().getItems().get(getIndex());
                Label badge;
                if (b.upcoming) {
                    badge = new Label("Upcoming");
                    badge.getStyleClass().add("status-badge-upcoming");
                } else if (b.overstay) {
                    badge = new Label("Overstay");
                    badge.getStyleClass().add("status-badge-overstay");
                } else {
                    badge = new Label("Active");
                    badge.getStyleClass().add("status-badge-ontime");
                }
                setGraphic(badge);
            }
        });
    }

    private void addActiveActionButtons() {
        colActiveAction.setCellFactory(new Callback<>() {
            @Override
            public TableCell<BookingApiService.BookingData, Void> call(TableColumn<BookingApiService.BookingData, Void> param) {
                return new TableCell<>() {
                    private final Button extendBtn = new Button("+1 hr");
                    private final Button completeBtn = new Button("Complete");
                    private final Button cancelBtn = new Button("Cancel");
                    private final HBox box = new HBox(6, extendBtn, completeBtn, cancelBtn);

                    {
                        extendBtn.setStyle("-fx-font-size: 11px; -fx-padding: 5 12 5 12; -fx-min-width: 0; " +
                                "-fx-background-color: #eef1f8; -fx-text-fill: #4361ee; -fx-effect: none;");
                        completeBtn.getStyleClass().add("customer-btn");
                        completeBtn.setStyle("-fx-font-size: 11px; -fx-padding: 5 12 5 12; -fx-min-width: 0;");
                        cancelBtn.getStyleClass().add("table-action-btn");

                        extendBtn.setOnAction(e -> {
                            BookingApiService.BookingData b = getTableView().getItems().get(getIndex());
                            handleExtend(b);
                        });
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
                        if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                            setGraphic(null);
                            return;
                        }
                        BookingApiService.BookingData b = getTableView().getItems().get(getIndex());
                        // an upcoming booking cannot be completed before its check-in time
                        completeBtn.setVisible(!b.upcoming);
                        completeBtn.setManaged(!b.upcoming);
                        setGraphic(box);
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
                    private final Button rateBtn = new Button("Rate");
                    private final HBox box = new HBox(6, receiptBtn);

                    {
                        receiptBtn.setStyle("-fx-font-size: 11px; -fx-padding: 5 12 5 12; -fx-min-width: 0; " +
                                "-fx-background-color: #eef1f8; -fx-text-fill: #4361ee; -fx-effect: none;");
                        rateBtn.setStyle("-fx-font-size: 11px; -fx-padding: 5 12 5 12; -fx-min-width: 0; " +
                                "-fx-background-color: #fff6e0; -fx-text-fill: #e67e22; -fx-effect: none;");

                        receiptBtn.setOnAction(e -> {
                            BookingApiService.BookingData b = getTableView().getItems().get(getIndex());
                            downloadReceipt(b);
                        });
                        rateBtn.setOnAction(e -> {
                            BookingApiService.BookingData b = getTableView().getItems().get(getIndex());
                            showRatingDialog(b);
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                            setGraphic(null);
                            return;
                        }
                        BookingApiService.BookingData b = getTableView().getItems().get(getIndex());
                        box.getChildren().setAll(receiptBtn);
                        if (b.status.equals("COMPLETED") && b.rating == null) {
                            box.getChildren().add(rateBtn);
                        }
                        setGraphic(box);
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

    private void handleExtend(BookingApiService.BookingData booking) {
        errorLabel.setText("");
        successLabel.setText("");

        new Thread(() -> {
            try {
                BookingApiService.extendBooking(booking.id);
                Platform.runLater(() -> {
                    loadBookings();
                    successLabel.setText("Check-out extended by 1 hour");
                });
            } catch (Exception e) {
                Platform.runLater(() -> errorLabel.setText("Failed: " + e.getMessage()));
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
                    loadBookings();
                    successLabel.setText("Booking completed! Fare calculated.");
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
                            loadBookings();
                            successLabel.setText("Booking cancelled");
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> errorLabel.setText("Failed: " + e.getMessage()));
                    }
                }).start();
            }
        });
    }

    private void showRatingDialog(BookingApiService.BookingData booking) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Rate Your Parking Experience");
        dialog.getDialogPane().getStylesheets().add(
                MainApp.class.getResource("/css/styles.css").toExternalForm()
        );
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        ComboBox<Integer> ratingCombo = new ComboBox<>(FXCollections.observableArrayList(1, 2, 3, 4, 5));
        ratingCombo.setValue(5);

        TextArea feedbackArea = new TextArea();
        feedbackArea.setPromptText("Optional feedback...");
        feedbackArea.setPrefRowCount(3);
        feedbackArea.setWrapText(true);

        VBox content = new VBox(10,
                new Label("Rating for slot " + booking.slotNumber + " (" + booking.vehicleNumber + ")"),
                ratingCombo,
                feedbackArea
        );
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Integer rating = ratingCombo.getValue();
                String feedback = feedbackArea.getText();

                new Thread(() -> {
                    try {
                        BookingApiService.rateBooking(booking.id, rating, feedback);
                        Platform.runLater(() -> {
                            loadBookings();
                            successLabel.setText("Thanks for your feedback!");
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

            String content = "PARKING MANAGEMENT SYSTEM - RECEIPT\n" +
                    "=====================================\n" +
                    "Booking ID: " + b.id + "\n" +
                    "Customer: " + SessionManager.getName() + "\n" +
                    "Vehicle: " + b.vehicleNumber + "\n" +
                    "Slot: " + b.slotNumber + "\n" +
                    "Check-in (booked): " + formatDateTime(b.entryTime) + "\n" +
                    "Check-out (booked): " + formatDateTime(b.expectedExitTime) + "\n" +
                    "Actual exit: " + (b.exitTime != null ? formatDateTime(b.exitTime) : "N/A") + "\n" +
                    "Status: " + b.status + "\n" +
                    "Estimated fare: " + (b.estimatedFare != null ? "₹" + b.estimatedFare : "N/A") + "\n" +
                    "Final fare: " + (b.fare != null ? "₹" + b.fare : "N/A") + "\n" +
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
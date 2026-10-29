package com.parking.client.controller;

import java.net.URL;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import com.parking.client.MainApp;
import com.parking.client.service.BookingApiService;
import com.parking.client.service.VehicleApiService;
import com.parking.client.util.QrCodeGenerator;
import com.parking.client.util.SessionManager;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

public class BookSlotController implements Initializable {

    @FXML private ComboBox<String> vehicleCombo;
    @FXML private Label rateLabel;
    @FXML private DatePicker checkInDate;
    @FXML private ComboBox<String> checkInTime;
    @FXML private DatePicker checkOutDate;
    @FXML private ComboBox<String> checkOutTime;
    @FXML private Label durationLabel;
    @FXML private FlowPane slotGridPane;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;
    @FXML private Button bookButton;

    private List<VehicleApiService.VehicleData> myVehicles;
    private List<BookingApiService.SlotData> availableSlots;
    private Long selectedSlotId;
    private VBox selectedBox;
    private double currentRate = 0;

    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        List<String> times = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            times.add(String.format("%02d:00", h));
            times.add(String.format("%02d:30", h));
        }
        checkInTime.setItems(FXCollections.observableArrayList(times));
        checkOutTime.setItems(FXCollections.observableArrayList(times));

        checkInDate.setEditable(false);
        checkOutDate.setEditable(false);
        disablePastDates();
        setDefaultTimes();

        checkInDate.valueProperty().addListener((o, a, b) -> {
            keepCheckOutAfterCheckIn();
            onTimesChanged();
        });
        checkInTime.valueProperty().addListener((o, a, b) -> onTimesChanged());
        checkOutDate.valueProperty().addListener((o, a, b) -> onTimesChanged());
        checkOutTime.valueProperty().addListener((o, a, b) -> onTimesChanged());

        vehicleCombo.setOnAction(this::handleVehicleSelected);
        loadVehicles();
        updateSummary();
    }

    // ---------------------------------------------------------------- date / time helpers

    private void disablePastDates() {
        checkInDate.setDayCellFactory(p -> new DateCell() {
            @Override
            public void updateItem(LocalDate d, boolean empty) {
                super.updateItem(d, empty);
                setDisable(empty || d.isBefore(LocalDate.now()));
            }
        });
        checkOutDate.setDayCellFactory(p -> new DateCell() {
            @Override
            public void updateItem(LocalDate d, boolean empty) {
                super.updateItem(d, empty);
                LocalDate min = checkInDate.getValue() != null ? checkInDate.getValue() : LocalDate.now();
                setDisable(empty || d.isBefore(min));
            }
        });
    }

    // default: check-in at the next half hour, check-out two hours later
    private void setDefaultTimes() {
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        LocalDateTime start = now.plusMinutes(30 - (now.getMinute() % 30));
        LocalDateTime end = start.plusHours(2);

        checkInDate.setValue(start.toLocalDate());
        checkInTime.setValue(start.format(TIME_FMT));
        checkOutDate.setValue(end.toLocalDate());
        checkOutTime.setValue(end.format(TIME_FMT));
    }

    private void keepCheckOutAfterCheckIn() {
        if (checkInDate.getValue() != null
                && (checkOutDate.getValue() == null || checkOutDate.getValue().isBefore(checkInDate.getValue()))) {
            checkOutDate.setValue(checkInDate.getValue());
        }
    }

    private LocalDateTime readDateTime(DatePicker date, ComboBox<String> time) {
        if (date.getValue() == null || time.getValue() == null) {
            return null;
        }
        return LocalDateTime.of(date.getValue(), LocalTime.parse(time.getValue()));
    }

    private String validateWindow(LocalDateTime in, LocalDateTime out) {
        if (in == null || out == null) return "Please select check-in and check-out date and time";
        if (in.isBefore(LocalDateTime.now().minusMinutes(5))) return "Check-in time cannot be in the past";
        if (!out.isAfter(in)) return "Check-out must be after check-in";
        if (Duration.between(in, out).toMinutes() < 60) return "Minimum booking duration is 1 hour";
        return null;
    }

    private String formatDuration(long minutes) {
        long days = minutes / 1440;
        long hours = (minutes % 1440) / 60;
        long mins = minutes % 60;
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append(days == 1 ? " day " : " days ");
        if (hours > 0) sb.append(hours).append(" h ");
        if (mins > 0) sb.append(mins).append(" min");
        return sb.toString().trim();
    }

    private String fmt(String iso) {
        if (iso == null) return "-";
        try {
            return LocalDateTime.parse(iso).format(DISPLAY);
        } catch (Exception e) {
            return iso.replace("T", " ");
        }
    }

    // changing the window makes the old slot list invalid, so it is cleared
    private void onTimesChanged() {
        slotGridPane.getChildren().clear();
        availableSlots = null;
        selectedSlotId = null;
        selectedBox = null;
        errorLabel.setText("");
        successLabel.setText("");
        updateSummary();
    }

    private void updateSummary() {
        LocalDateTime in = readDateTime(checkInDate, checkInTime);
        LocalDateTime out = readDateTime(checkOutDate, checkOutTime);

        if (in == null || out == null) {
            durationLabel.setText("");
            return;
        }

        long minutes = Duration.between(in, out).toMinutes();
        if (minutes <= 0) {
            durationLabel.setText("Check-out must be after check-in");
            return;
        }
        if (minutes < 60) {
            durationLabel.setText("Minimum booking duration is 1 hour");
            return;
        }

        String text = "Duration: " + formatDuration(minutes);
        if (currentRate > 0) {
            double hours = Math.ceil(minutes / 60.0);
            text += "   |   Estimated fare: ₹" + String.format("%.2f", hours * currentRate);
        }
        durationLabel.setText(text);
    }

    // ---------------------------------------------------------------- vehicles and rate

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

    private void handleVehicleSelected(ActionEvent event) {
        int selectedIndex = vehicleCombo.getSelectionModel().getSelectedIndex();
        if (selectedIndex < 0 || myVehicles == null) {
            rateLabel.setText("");
            currentRate = 0;
            updateSummary();
            return;
        }

        String vehicleType = myVehicles.get(selectedIndex).vehicleType;
        onTimesChanged();

        new Thread(() -> {
            try {
                double rate = BookingApiService.getRate(vehicleType);
                Platform.runLater(() -> {
                    currentRate = rate;
                    rateLabel.setText("Rate: ₹" + rate + " / hour (minimum 1 hour)");
                    updateSummary();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    currentRate = 0;
                    rateLabel.setText("");
                    updateSummary();
                });
            }
        }).start();
    }

    // ---------------------------------------------------------------- slots and booking

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

        final LocalDateTime in = readDateTime(checkInDate, checkInTime);
        final LocalDateTime out = readDateTime(checkOutDate, checkOutTime);

        String problem = validateWindow(in, out);
        if (problem != null) {
            errorLabel.setText(problem);
            return;
        }

        String vehicleType = myVehicles.get(selectedIndex).vehicleType;

        new Thread(() -> {
            try {
                availableSlots = BookingApiService.getAvailableSlots(vehicleType, in, out);
                Platform.runLater(this::renderSlotGrid);
            } catch (Exception e) {
                Platform.runLater(() -> errorLabel.setText("Failed: " + e.getMessage()));
            }
        }).start();
    }

    private void renderSlotGrid() {
        slotGridPane.getChildren().clear();

        if (availableSlots == null || availableSlots.isEmpty()) {
            errorLabel.setText("No slots are free for this time. Try a different time or date.");
            return;
        }

        for (BookingApiService.SlotData slot : availableSlots) {
            VBox box = new VBox();
            box.setAlignment(Pos.CENTER);
            box.getStyleClass().addAll("slot-box", "slot-free");
            box.setPrefSize(70, 60);

            box.getChildren().add(new Label(slot.slotNumber));
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

        final LocalDateTime in = readDateTime(checkInDate, checkInTime);
        final LocalDateTime out = readDateTime(checkOutDate, checkOutTime);

        String problem = validateWindow(in, out);
        if (problem != null) {
            errorLabel.setText(problem);
            return;
        }

        if (selectedSlotId == null) {
            errorLabel.setText("Please check available slots and tap a slot first");
            return;
        }

        final Long vehicleId = myVehicles.get(vehicleIndex).id;
        final String vehicleType = myVehicles.get(vehicleIndex).vehicleType;
        final String vehicleLabel = vehicleCombo.getValue();
        final Long slotId = selectedSlotId;

        bookButton.setDisable(true);
        bookButton.setText("Booking...");

        new Thread(() -> {
            try {
                BookingApiService.BookingData booking =
                        BookingApiService.bookSlot(vehicleId, vehicleType, slotId, in, out);
                Platform.runLater(() -> {
                    bookButton.setDisable(false);
                    bookButton.setText("Book Selected Slot");
                    slotGridPane.getChildren().clear();
                    selectedSlotId = null;
                    selectedBox = null;
                    successLabel.setText("Slot booked successfully!");
                    showTicketConfirmation(booking, vehicleLabel);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    bookButton.setDisable(false);
                    bookButton.setText("Book Selected Slot");
                    errorLabel.setText("Booking failed: " + e.getMessage());
                });
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
        content.getChildren().add(ticketIdLabel);

        String qrPayload = "BOOKING:" + booking.id + "|SLOT:" + booking.slotNumber + "|VEHICLE:" + vehicleLabel
                + "|IN:" + booking.entryTime + "|OUT:" + booking.expectedExitTime;
        Image qrImage = QrCodeGenerator.generate(qrPayload, 140);
        if (qrImage != null) {
            ImageView qrView = new ImageView(qrImage);
            qrView.setFitWidth(140);
            qrView.setFitHeight(140);
            content.getChildren().add(qrView);
        }

        content.getChildren().addAll(
                new Label("Slot: " + booking.slotNumber),
                new Label("Vehicle: " + vehicleLabel),
                new Label("Check-in: " + fmt(booking.entryTime)),
                new Label("Check-out: " + fmt(booking.expectedExitTime)),
                new Label("Estimated Fare: ₹" + (booking.estimatedFare != null ? String.format("%.2f", booking.estimatedFare) : "-")),
                new Label("Status: " + (booking.upcoming ? "Upcoming" : "Active"))
        );

        alert.getDialogPane().setContent(content);
        alert.getDialogPane().getStylesheets().add(
                MainApp.class.getResource("/css/styles.css").toExternalForm()
        );
        alert.getButtonTypes().setAll(ButtonType.OK);
        alert.showAndWait();
    }

    // ---------------------------------------------------------------- navigation

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
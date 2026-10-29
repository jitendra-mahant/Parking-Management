package com.parking.client.controller;

import com.parking.client.MainApp;
import com.parking.client.service.BookingApiService;
import com.parking.client.service.StaffApiService;
import com.parking.client.util.SessionManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class StaffDashboardController implements Initializable {

    @FXML private Label welcomeLabel;
    @FXML private Label avatarLabel;
    @FXML private Label sidebarUserName;
    @FXML private Label sidebarUserRole;
    @FXML private Label totalSlotsLabel;
    @FXML private Label occupiedSlotsLabel;
    @FXML private Label availableSlotsLabel;
    @FXML private Label todayEntriesLabel;
    @FXML private Label todayExitsLabel;
    @FXML private Label overstayLabel;

    @FXML private VBox slotGridPane;
    @FXML private ComboBox<String> zoneFilterCombo;
    @FXML private ComboBox<String> statusFilterCombo;

    @FXML private TextField entryVehicleNumberField;
    @FXML private ComboBox<String> entryVehicleTypeCombo;
    @FXML private Label entryStatusLabel;

    @FXML private TextField activeBookingSearchField;
    @FXML private TableView<BookingApiService.BookingData> activeBookingsTable;
    @FXML private TableColumn<BookingApiService.BookingData, String> colStaffSlot;
    @FXML private TableColumn<BookingApiService.BookingData, String> colStaffVehicle;
    @FXML private TableColumn<BookingApiService.BookingData, String> colStaffEntry;
    @FXML private TableColumn<BookingApiService.BookingData, Void> colStaffStatus;
    @FXML private TableColumn<BookingApiService.BookingData, Void> colStaffAction;

    @FXML private Label exitStatusLabel;

    private final ObservableList<BookingApiService.BookingData> masterActiveBookings = FXCollections.observableArrayList();
    private List<StaffApiService.SlotInfo> cachedSlots = List.of();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        String name = SessionManager.getName();
        String role = SessionManager.getRole();
        if (name == null || name.isEmpty()) name = "Operator";
        if (role == null || role.isEmpty()) role = "staff";

        welcomeLabel.setText("Operations Dashboard — " + name);
        if (sidebarUserName != null) sidebarUserName.setText(name);
        if (sidebarUserRole != null) sidebarUserRole.setText("STAFF OPERATOR");
        avatarLabel.setText(name.substring(0, 1).toUpperCase());

        entryVehicleTypeCombo.setItems(FXCollections.observableArrayList("Car", "Bike", "Truck"));
        entryVehicleTypeCombo.setValue("Car");

        if (zoneFilterCombo != null) {
            zoneFilterCombo.setItems(FXCollections.observableArrayList("All Zones", "Car", "Bike", "Truck"));
            zoneFilterCombo.setValue("All Zones");
            zoneFilterCombo.setOnAction(e -> renderSlotGrid());
        }

        if (statusFilterCombo != null) {
            statusFilterCombo.setItems(FXCollections.observableArrayList("All Statuses", "Available Only", "Occupied Only"));
            statusFilterCombo.setValue("All Statuses");
            statusFilterCombo.setOnAction(e -> renderSlotGrid());
        }

        colStaffSlot.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().slotNumber));
        colStaffVehicle.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().vehicleNumber));
        colStaffEntry.setCellValueFactory(d -> new SimpleStringProperty(formatDateTime(d.getValue().entryTime)));
        addStatusColumn();
        addExitButtonColumn();
        addOverstayRowHighlight();

        FilteredList<BookingApiService.BookingData> filtered = new FilteredList<>(masterActiveBookings, b -> true);
        activeBookingsTable.setItems(filtered);

        if (activeBookingSearchField != null) {
            activeBookingSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
                String query = newVal == null ? "" : newVal.trim().toLowerCase();
                filtered.setPredicate(b -> query.isEmpty()
                        || (b.vehicleNumber != null && b.vehicleNumber.toLowerCase().contains(query))
                        || (b.slotNumber != null && b.slotNumber.toLowerCase().contains(query)));
            });
        }

        // Auto-uppercase vehicle number field
        entryVehicleNumberField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(newVal.toUpperCase())) {
                entryVehicleNumberField.setText(newVal.toUpperCase());
            }
        });

        refreshAll();
    }

    private String formatDateTime(String iso) {
        if (iso == null) return "-";
        String cleaned = iso.replace("T", " ");
        return cleaned.length() >= 16 ? cleaned.substring(0, 16) : cleaned;
    }

    private void addStatusColumn() {
        colStaffStatus.setCellFactory(col -> new TableCell<>() {
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
                    badge = new Label("Parked");
                    badge.getStyleClass().add("status-badge-ontime");
                }
                setGraphic(badge);
            }
        });
    }

    private void addExitButtonColumn() {
        colStaffAction.setCellFactory(new Callback<>() {
            @Override
            public TableCell<BookingApiService.BookingData, Void> call(TableColumn<BookingApiService.BookingData, Void> param) {
                return new TableCell<>() {
                    private final Button exitBtn = new Button("Process Exit");

                    {
                        exitBtn.getStyleClass().add("table-action-btn-primary");
                        exitBtn.setOnAction(e -> {
                            BookingApiService.BookingData b = getTableView().getItems().get(getIndex());
                            confirmAndProcessExit(b);
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
                        setGraphic(b.upcoming ? null : exitBtn);
                    }
                };
            }
        });
    }

    private void addOverstayRowHighlight() {
        activeBookingsTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(BookingApiService.BookingData item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().remove("table-row-overstay");
                if (item != null && !empty && item.overstay) {
                    getStyleClass().add("table-row-overstay");
                }
            }
        });
    }

    @FXML
    public void refreshAll() {
        if (exitStatusLabel != null) exitStatusLabel.setText("");
        loadQuickStats();
        loadSlotGrid();
        loadActiveBookings();
    }

    private void loadQuickStats() {
        new Thread(() -> {
            try {
                StaffApiService.QuickStats stats = StaffApiService.getQuickStats();
                Platform.runLater(() -> {
                    totalSlotsLabel.setText(String.valueOf(stats.totalSlots));
                    occupiedSlotsLabel.setText(String.valueOf(stats.occupiedSlots));
                    availableSlotsLabel.setText(String.valueOf(stats.availableSlots));
                    todayEntriesLabel.setText(String.valueOf(stats.todayEntries));
                    todayExitsLabel.setText(String.valueOf(stats.todayExits));
                    overstayLabel.setText(String.valueOf(stats.overstayCount));
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (exitStatusLabel != null) exitStatusLabel.setText("Failed to load stats: " + e.getMessage());
                });
            }
        }).start();
    }

    private void loadSlotGrid() {
        new Thread(() -> {
            try {
                cachedSlots = StaffApiService.getAllSlots();
                Platform.runLater(this::renderSlotGrid);
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (exitStatusLabel != null) exitStatusLabel.setText("Failed to load slots: " + e.getMessage());
                });
            }
        }).start();
    }

    private void renderSlotGrid() {
        if (slotGridPane == null) return;
        slotGridPane.getChildren().clear();

        String selectedZone = zoneFilterCombo != null ? zoneFilterCombo.getValue() : "All Zones";
        String selectedStatus = statusFilterCombo != null ? statusFilterCombo.getValue() : "All Statuses";

        String[] zones = {"Car", "Bike", "Truck"};
        for (String zone : zones) {
            if (!"All Zones".equals(selectedZone) && !zone.equals(selectedZone)) continue;

            Label zoneLabel = new Label(zone + " Zone (" + (zone.equals("Car") ? "A" : zone.equals("Bike") ? "B" : "T") + " Series)");
            zoneLabel.setStyle("-fx-font-weight: 800; -fx-font-size: 13px; -fx-text-fill: #14213D;");

            javafx.scene.layout.FlowPane zoneRow = new javafx.scene.layout.FlowPane();
            zoneRow.getStyleClass().add("slot-grid");

            int zoneCount = 0;
            for (StaffApiService.SlotInfo slot : cachedSlots) {
                if (!slot.vehicleType.equalsIgnoreCase(zone)) continue;
                if ("Available Only".equals(selectedStatus) && slot.occupied) continue;
                if ("Occupied Only".equals(selectedStatus) && !slot.occupied) continue;

                zoneCount++;
                VBox box = new VBox(2);
                box.setAlignment(Pos.CENTER);
                box.getStyleClass().add("slot-box");
                box.getStyleClass().add(slot.occupied ? "slot-occupied" : "slot-free");

                Label numLabel = new Label(slot.slotNumber);
                Label typeLabel = new Label(slot.occupied ? "OCCUPIED" : "AVAILABLE");
                typeLabel.setStyle("-fx-font-size: 9px; -fx-font-weight: 700; -fx-text-fill: " + (slot.occupied ? "#D64545" : "#21865B") + ";");

                box.getChildren().addAll(numLabel, typeLabel);

                // Clicking an occupied slot filters active bookings table to that slot!
                box.setOnMouseClicked(ev -> {
                    if (activeBookingSearchField != null) {
                        activeBookingSearchField.setText(slot.slotNumber);
                    }
                });

                zoneRow.getChildren().add(box);
            }

            if (zoneCount > 0) {
                VBox zoneBlock = new VBox(6, zoneLabel, zoneRow);
                slotGridPane.getChildren().add(zoneBlock);
            }
        }
    }

    @FXML
    private void handleRecordEntry() {
        String vehicleNumber = entryVehicleNumberField.getText() != null ? entryVehicleNumberField.getText().trim() : "";
        String vehicleType = entryVehicleTypeCombo.getValue();

        entryStatusLabel.setText("");

        if (vehicleNumber.isEmpty() || vehicleType == null) {
            entryStatusLabel.setStyle("-fx-text-fill: #D64545; -fx-font-weight: bold;");
            entryStatusLabel.setText("Please enter vehicle number and select vehicle type");
            return;
        }

        new Thread(() -> {
            try {
                BookingApiService.BookingData result = StaffApiService.recordEntry(vehicleNumber, vehicleType);
                Platform.runLater(() -> {
                    entryVehicleNumberField.clear();
                    refreshAll();
                    entryStatusLabel.setStyle("-fx-text-fill: #21865B; -fx-font-weight: bold;");
                    entryStatusLabel.setText("Vehicle " + result.vehicleNumber + " checked in successfully! Assigned Slot: " + result.slotNumber);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    entryStatusLabel.setStyle("-fx-text-fill: #D64545; -fx-font-weight: bold;");
                    entryStatusLabel.setText("Check-in failed: " + e.getMessage());
                });
            }
        }).start();
    }

    private void loadActiveBookings() {
        new Thread(() -> {
            try {
                List<BookingApiService.BookingData> bookings = StaffApiService.getAllActiveBookings();
                Platform.runLater(() -> masterActiveBookings.setAll(bookings));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (exitStatusLabel != null) exitStatusLabel.setText("Failed to load active sessions: " + e.getMessage());
                });
            }
        }).start();
    }

    private void confirmAndProcessExit(BookingApiService.BookingData booking) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Process Vehicle Exit");
        confirm.setHeaderText(null);
        confirm.setContentText("Process exit for Vehicle " + booking.vehicleNumber + " at Slot " + booking.slotNumber + "?\n\nEntry Time: " + formatDateTime(booking.entryTime) + "\nThis will calculate the final fare and free the slot.");
        confirm.getDialogPane().getStylesheets().add(MainApp.class.getResource("/css/styles.css").toExternalForm());

        confirm.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) {
                processExit(booking);
            }
        });
    }

    private void processExit(BookingApiService.BookingData booking) {
        if (exitStatusLabel != null) exitStatusLabel.setText("");

        new Thread(() -> {
            try {
                BookingApiService.BookingData result = StaffApiService.recordExit(booking.id);
                Platform.runLater(() -> {
                    refreshAll();
                    showExitReceipt(result);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (exitStatusLabel != null) {
                        exitStatusLabel.setStyle("-fx-text-fill: #D64545; -fx-font-weight: bold;");
                        exitStatusLabel.setText("Exit processing failed: " + e.getMessage());
                    }
                });
            }
        }).start();
    }

    private void showExitReceipt(BookingApiService.BookingData result) {
        Alert receipt = new Alert(Alert.AlertType.INFORMATION);
        receipt.setTitle("Vehicle Exit Completed");
        receipt.setHeaderText(null);

        VBox content = new VBox(10);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setStyle("-fx-padding: 16;");

        Label title = new Label("Vehicle Exit Receipt");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: #14213D;");

        Label slot = new Label("Released Slot: " + result.slotNumber);
        slot.setStyle("-fx-font-weight: 700;");

        Label vehicle = new Label("Vehicle: " + result.vehicleNumber);
        Label entry = new Label("Check-In: " + formatDateTime(result.entryTime));
        Label exit = new Label("Check-Out: " + formatDateTime(result.exitTime));

        Label fare = new Label("Total Fare Collected: ₹" + (result.fare != null ? String.format("%.2f", result.fare) : "0.00"));
        fare.setStyle("-fx-font-size: 15px; -fx-font-weight: 800; -fx-text-fill: #21865B; -fx-padding: 6 0 0 0;");

        content.getChildren().addAll(title, slot, vehicle, entry, exit, fare);
        receipt.getDialogPane().setContent(content);
        receipt.getDialogPane().getStylesheets().add(MainApp.class.getResource("/css/styles.css").toExternalForm());
        receipt.showAndWait();
    }

    @FXML
    private void goToDashboard() {
        try { MainApp.switchScene("/fxml/staff_dashboard.fxml"); } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void goToProfile() {
        try { MainApp.switchScene("/fxml/profile.fxml"); } catch (Exception e) { e.printStackTrace(); }
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

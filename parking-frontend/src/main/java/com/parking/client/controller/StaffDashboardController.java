package com.parking.client.controller;

import com.parking.client.MainApp;
import com.parking.client.service.BookingApiService;
import com.parking.client.service.StaffApiService;
import com.parking.client.util.SessionManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class StaffDashboardController implements Initializable {

    @FXML private Label welcomeLabel;
    @FXML private Label avatarLabel;
    @FXML private Label totalSlotsLabel;
    @FXML private Label occupiedSlotsLabel;
    @FXML private Label availableSlotsLabel;
    @FXML private Label todayEntriesLabel;
    @FXML private Label todayExitsLabel;

    @FXML private VBox slotGridPane;

    @FXML private TextField entryVehicleNumberField;
    @FXML private ComboBox<String> entryVehicleTypeCombo;
    @FXML private Label entryStatusLabel;

    @FXML private TableView<BookingApiService.BookingData> activeBookingsTable;
    @FXML private TableColumn<BookingApiService.BookingData, String> colStaffSlot;
    @FXML private TableColumn<BookingApiService.BookingData, String> colStaffVehicle;
    @FXML private TableColumn<BookingApiService.BookingData, String> colStaffEntry;
    @FXML private TableColumn<BookingApiService.BookingData, Void> colStaffAction;

    @FXML private Label exitStatusLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        String name = SessionManager.getName();
        welcomeLabel.setText("Staff Dashboard - " + name);
        avatarLabel.setText(name != null && !name.isEmpty() ? name.substring(0, 1).toUpperCase() : "S");
        entryVehicleTypeCombo.setItems(FXCollections.observableArrayList("Car", "Bike", "Truck"));

        colStaffSlot.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().slotNumber));
        colStaffVehicle.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().vehicleNumber));
        colStaffEntry.setCellValueFactory(d -> new SimpleStringProperty(formatDateTime(d.getValue().entryTime)));
        addExitButtonColumn();

        refreshAll();
    }

    private String formatDateTime(String iso) {
        if (iso == null) return "-";
        return iso.replace("T", " ").length() >= 16 ? iso.replace("T", " ").substring(0, 16) : iso;
    }

    private void addExitButtonColumn() {
        colStaffAction.setCellFactory(new Callback<>() {
            @Override
            public TableCell<BookingApiService.BookingData, Void> call(TableColumn<BookingApiService.BookingData, Void> param) {
                return new TableCell<>() {
                    private final Button exitBtn = new Button("Exit");

                    {
                        exitBtn.getStyleClass().add("staff-btn");
                        exitBtn.setStyle("-fx-font-size: 11px; -fx-padding: 5 14 5 14; -fx-min-width: 0;");
                        exitBtn.setOnAction(e -> {
                            BookingApiService.BookingData b = getTableView().getItems().get(getIndex());
                            recordExit(b);
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        setGraphic(empty ? null : exitBtn);
                    }
                };
            }
        });
    }

    @FXML
    private void refreshAll() {
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
                });
            } catch (Exception e) {
                Platform.runLater(() -> exitStatusLabel.setText("Failed to load stats: " + e.getMessage()));
            }
        }).start();
    }

    private void loadSlotGrid() {
        new Thread(() -> {
            try {
                List<StaffApiService.SlotInfo> slots = StaffApiService.getAllSlots();
                Platform.runLater(() -> {
                    slotGridPane.getChildren().clear();

                    String[] zones = {"Car", "Bike", "Truck"};
                    for (String zone : zones) {
                        Label zoneLabel = new Label(zone + " Zone");
                        zoneLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #5a6478;");

                        javafx.scene.layout.FlowPane zoneRow = new javafx.scene.layout.FlowPane();
                        zoneRow.getStyleClass().add("slot-grid");

                        for (StaffApiService.SlotInfo slot : slots) {
                            if (!slot.vehicleType.equals(zone)) continue;

                            VBox box = new VBox();
                            box.setAlignment(Pos.CENTER);
                            box.getStyleClass().add("slot-box");
                            box.getStyleClass().add(slot.occupied ? "slot-occupied" : "slot-free");

                            Label numberLabel = new Label(slot.slotNumber);
                            box.getChildren().add(numberLabel);

                            zoneRow.getChildren().add(box);
                        }

                        VBox zoneBlock = new VBox(6, zoneLabel, zoneRow);
                        slotGridPane.getChildren().add(zoneBlock);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> exitStatusLabel.setText("Failed to load slots: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleRecordEntry() {
        String vehicleNumber = entryVehicleNumberField.getText();
        String vehicleType = entryVehicleTypeCombo.getValue();

        entryStatusLabel.setText("");

        if (vehicleNumber.isEmpty() || vehicleType == null) {
            entryStatusLabel.setText("Please enter vehicle number and select type");
            return;
        }

        new Thread(() -> {
            try {
                StaffApiService.recordEntry(vehicleNumber, vehicleType);
                Platform.runLater(() -> {
                    entryStatusLabel.setText("Entry recorded successfully");
                    entryVehicleNumberField.clear();
                    entryVehicleTypeCombo.setValue(null);
                    refreshAll();
                });
            } catch (Exception e) {
                Platform.runLater(() -> entryStatusLabel.setText("Failed: " + e.getMessage()));
            }
        }).start();
    }

    private void loadActiveBookings() {
        new Thread(() -> {
            try {
                List<BookingApiService.BookingData> bookings = StaffApiService.getAllActiveBookings();
                Platform.runLater(() -> activeBookingsTable.setItems(FXCollections.observableArrayList(bookings)));
            } catch (Exception e) {
                Platform.runLater(() -> exitStatusLabel.setText("Failed to load bookings: " + e.getMessage()));
            }
        }).start();
    }

    private void recordExit(BookingApiService.BookingData booking) {
        exitStatusLabel.setText("");

        new Thread(() -> {
            try {
                BookingApiService.BookingData result = StaffApiService.recordExit(booking.id);
                Platform.runLater(() -> {
                    exitStatusLabel.setText("Exit recorded. Fare: ₹" + result.fare);
                    refreshAll();
                });
            } catch (Exception e) {
                Platform.runLater(() -> exitStatusLabel.setText("Failed: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void goToDashboard() {
        try { MainApp.switchScene("/fxml/staff_dashboard.fxml"); } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void handleLogout() {
        SessionManager.clearSession();
        try { MainApp.switchScene("/fxml/login.fxml"); } catch (Exception e) { e.printStackTrace(); }
    }
}
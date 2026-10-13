package com.parking.client.controller;

import com.parking.client.MainApp;
import com.parking.client.service.AdminApiService;
import com.parking.client.service.BookingApiService;
import com.parking.client.util.SessionManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.util.Callback;

import java.io.FileWriter;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;
import java.util.ResourceBundle;

public class AdminDashboardController implements Initializable {

    @FXML private Label welcomeLabel;
    @FXML private Label avatarLabel;
    @FXML private Label revenueLabel;
    @FXML private BarChart<String, Number> revenueChart;

    @FXML private TextField staffNameField;
    @FXML private TextField staffEmailField;
    @FXML private PasswordField staffPasswordField;
    @FXML private TextField staffPhoneField;
    @FXML private Label staffStatusLabel;

    @FXML private TableView<AdminApiService.StaffData> staffTable;
    @FXML private TableColumn<AdminApiService.StaffData, String> colStaffName;
    @FXML private TableColumn<AdminApiService.StaffData, String> colStaffEmail;
    @FXML private TableColumn<AdminApiService.StaffData, String> colStaffPhone;
    @FXML private TableColumn<AdminApiService.StaffData, Void> colStaffDelete;

    @FXML private TextField bookingSearchField;
    @FXML private TableView<BookingApiService.BookingData> bookingsTable;
    @FXML private TableColumn<BookingApiService.BookingData, String> colBookSlot;
    @FXML private TableColumn<BookingApiService.BookingData, String> colBookVehicle;
    @FXML private TableColumn<BookingApiService.BookingData, String> colBookStatus;
    @FXML private TableColumn<BookingApiService.BookingData, String> colBookFare;

    private final ObservableList<BookingApiService.BookingData> allBookings = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        String name = SessionManager.getName();
        welcomeLabel.setText("Admin Dashboard - " + name);
        avatarLabel.setText(name != null && !name.isEmpty() ? name.substring(0, 1).toUpperCase() : "A");

        colStaffName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().name));
        colStaffEmail.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().email));
        colStaffPhone.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().phone == null || d.getValue().phone.isEmpty() ? "-" : d.getValue().phone));
        addStaffDeleteButton();

        colBookSlot.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().slotNumber));
        colBookVehicle.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().vehicleNumber));
        colBookStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().status));
        colBookFare.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().fare != null ? "₹" + d.getValue().fare : "-"));

        FilteredList<BookingApiService.BookingData> filtered = new FilteredList<>(allBookings, b -> true);
        bookingsTable.setItems(filtered);

        bookingSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String query = newVal == null ? "" : newVal.trim().toLowerCase();
            filtered.setPredicate(b -> query.isEmpty() || b.vehicleNumber.toLowerCase().contains(query));
        });

        loadRevenue();
        loadStaff();
        loadBookings();
    }

    private void addStaffDeleteButton() {
        colStaffDelete.setCellFactory(new Callback<>() {
            @Override
            public TableCell<AdminApiService.StaffData, Void> call(TableColumn<AdminApiService.StaffData, Void> param) {
                return new TableCell<>() {
                    private final Button deleteBtn = new Button("Remove");

                    {
                        deleteBtn.getStyleClass().add("table-action-btn");
                        deleteBtn.setOnAction(e -> {
                            AdminApiService.StaffData staff = getTableView().getItems().get(getIndex());
                            confirmAndRemove(staff);
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        setGraphic(empty ? null : deleteBtn);
                    }
                };
            }
        });
    }

    private void confirmAndRemove(AdminApiService.StaffData staff) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Remove Staff");
        confirm.setHeaderText(null);
        confirm.setContentText("Remove staff member " + staff.name + " (" + staff.email + ")?");
        confirm.getDialogPane().getStylesheets().add(
                MainApp.class.getResource("/css/styles.css").toExternalForm()
        );

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                new Thread(() -> {
                    try {
                        AdminApiService.deleteStaff(staff.id);
                        Platform.runLater(() -> {
                            loadStaff();
                            staffStatusLabel.setText("Staff removed");
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> staffStatusLabel.setText("Failed: " + e.getMessage()));
                    }
                }).start();
            }
        });
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

        loadRevenueChart();
    }

    private void loadRevenueChart() {
        new Thread(() -> {
            try {
                List<AdminApiService.DailyRevenueData> data = AdminApiService.getRevenueChart();
                Platform.runLater(() -> {
                    XYChart.Series<String, Number> series = new XYChart.Series<>();
                    for (AdminApiService.DailyRevenueData d : data) {
                        series.getData().add(new XYChart.Data<>(d.date, d.revenue));
                    }
                    revenueChart.getData().clear();
                    revenueChart.getData().add(series);
                });
            } catch (Exception e) {
                Platform.runLater(() -> staffStatusLabel.setText("Failed to load chart: " + e.getMessage()));
            }
        }).start();
    }

    private void loadStaff() {
        new Thread(() -> {
            try {
                var staffList = AdminApiService.getAllStaff();
                Platform.runLater(() -> staffTable.setItems(FXCollections.observableArrayList(staffList)));
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
    private void loadBookings() {
        new Thread(() -> {
            try {
                var bookings = AdminApiService.getAllBookings();
                Platform.runLater(() -> allBookings.setAll(bookings));
            } catch (Exception e) {
                Platform.runLater(() -> staffStatusLabel.setText("Failed to load bookings: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleExportCsv() {
        staffStatusLabel.setText("");

        var bookings = bookingsTable.getItems();
        if (bookings == null || bookings.isEmpty()) {
            staffStatusLabel.setText("No bookings to export");
            return;
        }

        try {
            String fileName = "Bookings_Export_" + System.currentTimeMillis() + ".csv";
            String userHome = System.getProperty("user.home");
            String filePath = Paths.get(userHome, "Downloads", fileName).toString();

            StringBuilder csv = new StringBuilder();
            csv.append("Booking ID,Slot,Vehicle,Status,Entry Time,Exit Time,Fare\n");

            for (BookingApiService.BookingData b : bookings) {
                csv.append(b.id).append(",")
                   .append(b.slotNumber).append(",")
                   .append(b.vehicleNumber).append(",")
                   .append(b.status).append(",")
                   .append(b.entryTime != null ? b.entryTime : "").append(",")
                   .append(b.exitTime != null ? b.exitTime : "").append(",")
                   .append(b.fare != null ? b.fare : "")
                   .append("\n");
            }

            try (FileWriter writer = new FileWriter(filePath)) {
                writer.write(csv.toString());
            }

            staffStatusLabel.setText("Exported " + bookings.size() + " bookings to Downloads: " + fileName);
        } catch (Exception e) {
            staffStatusLabel.setText("Export failed: " + e.getMessage());
        }
    }

    @FXML
    private void goToDashboard() {
        try { MainApp.switchScene("/fxml/admin_dashboard.fxml"); } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void handleLogout() {
        SessionManager.clearSession();
        try { MainApp.switchScene("/fxml/login.fxml"); } catch (Exception e) { e.printStackTrace(); }
    }
}
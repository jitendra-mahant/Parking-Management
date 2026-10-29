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
    @FXML private Label sidebarUserName;
    @FXML private Label sidebarUserRole;

    @FXML private Label totalRevenueKpi;
    @FXML private Label totalBookingsKpi;
    @FXML private Label activeBookingsKpi;
    @FXML private Label completedBookingsKpi;
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
    @FXML private ComboBox<String> bookingStatusFilterCombo;
    @FXML private TableView<BookingApiService.BookingData> bookingsTable;
    @FXML private TableColumn<BookingApiService.BookingData, String> colBookSlot;
    @FXML private TableColumn<BookingApiService.BookingData, String> colBookVehicle;
    @FXML private TableColumn<BookingApiService.BookingData, String> colBookCheckIn;
    @FXML private TableColumn<BookingApiService.BookingData, String> colBookCheckOut;
    @FXML private TableColumn<BookingApiService.BookingData, Void> colBookStatus;
    @FXML private TableColumn<BookingApiService.BookingData, String> colBookFare;

    private final ObservableList<BookingApiService.BookingData> allBookings = FXCollections.observableArrayList();
    private FilteredList<BookingApiService.BookingData> filteredBookings;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        String name = SessionManager.getName();
        String role = SessionManager.getRole();
        if (name == null || name.isEmpty()) name = "Administrator";
        if (role == null || role.isEmpty()) role = "admin";

        welcomeLabel.setText("System Administration Console — " + name);
        if (sidebarUserName != null) sidebarUserName.setText(name);
        if (sidebarUserRole != null) sidebarUserRole.setText("SYSTEM ADMIN");
        avatarLabel.setText(name.substring(0, 1).toUpperCase());

        // Setup Staff Table
        colStaffName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().name));
        colStaffEmail.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().email));
        colStaffPhone.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().phone == null || d.getValue().phone.isEmpty() ? "-" : d.getValue().phone));
        addStaffDeleteButton();

        // Setup Bookings Table
        colBookSlot.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().slotNumber));
        colBookVehicle.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().vehicleNumber));
        if (colBookCheckIn != null) {
            colBookCheckIn.setCellValueFactory(d -> new SimpleStringProperty(formatDateTime(d.getValue().entryTime)));
        }
        if (colBookCheckOut != null) {
            colBookCheckOut.setCellValueFactory(d -> new SimpleStringProperty(formatDateTime(
                    d.getValue().exitTime != null ? d.getValue().exitTime : d.getValue().expectedExitTime)));
        }
        colBookFare.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().fare != null ? "₹" + String.format("%.2f", d.getValue().fare) :
                (d.getValue().estimatedFare != null ? "₹" + String.format("%.2f", d.getValue().estimatedFare) : "-")));

        addBookingStatusColumn();

        filteredBookings = new FilteredList<>(allBookings, b -> true);
        bookingsTable.setItems(filteredBookings);

        if (bookingStatusFilterCombo != null) {
            bookingStatusFilterCombo.setItems(FXCollections.observableArrayList("All Statuses", "ACTIVE", "COMPLETED", "CANCELLED"));
            bookingStatusFilterCombo.setValue("All Statuses");
            bookingStatusFilterCombo.setOnAction(e -> applyBookingFilters());
        }

        bookingSearchField.textProperty().addListener((obs, oldVal, newVal) -> applyBookingFilters());

        loadRevenue();
        loadStaff();
        loadBookings();
    }

    private String formatDateTime(String iso) {
        if (iso == null) return "-";
        String cleaned = iso.replace("T", " ");
        return cleaned.length() >= 16 ? cleaned.substring(0, 16) : cleaned;
    }

    private void applyBookingFilters() {
        String query = bookingSearchField.getText() != null ? bookingSearchField.getText().trim().toLowerCase() : "";
        String status = bookingStatusFilterCombo != null ? bookingStatusFilterCombo.getValue() : "All Statuses";

        filteredBookings.setPredicate(b -> {
            boolean matchesQuery = query.isEmpty()
                    || (b.vehicleNumber != null && b.vehicleNumber.toLowerCase().contains(query))
                    || (b.slotNumber != null && b.slotNumber.toLowerCase().contains(query));

            boolean matchesStatus = status == null || "All Statuses".equalsIgnoreCase(status)
                    || (b.status != null && b.status.equalsIgnoreCase(status));

            return matchesQuery && matchesStatus;
        });
    }

    private void addBookingStatusColumn() {
        colBookStatus.setCellFactory(col -> new TableCell<>() {
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
                } else {
                    badge.getStyleClass().add("status-badge-ontime");
                }
                setGraphic(badge);
            }
        });
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
        confirm.setTitle("Remove Staff Member");
        confirm.setHeaderText(null);
        confirm.setContentText("Are you sure you want to remove staff operator " + staff.name + " (" + staff.email + ")?");
        confirm.getDialogPane().getStylesheets().add(MainApp.class.getResource("/css/styles.css").toExternalForm());

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                new Thread(() -> {
                    try {
                        AdminApiService.deleteStaff(staff.id);
                        Platform.runLater(() -> {
                            loadStaff();
                            staffStatusLabel.setStyle("-fx-text-fill: #21865B; -fx-font-weight: bold;");
                            staffStatusLabel.setText("Staff member " + staff.name + " removed successfully.");
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            staffStatusLabel.setStyle("-fx-text-fill: #D64545; -fx-font-weight: bold;");
                            staffStatusLabel.setText("Removal failed: " + e.getMessage());
                        });
                    }
                }).start();
            }
        });
    }

    @FXML
    public void loadRevenue() {
        new Thread(() -> {
            try {
                AdminApiService.RevenueData data = AdminApiService.getRevenueReport();
                Platform.runLater(() -> {
                    if (totalRevenueKpi != null) totalRevenueKpi.setText("₹" + String.format("%.2f", data.totalRevenue));
                    if (totalBookingsKpi != null) totalBookingsKpi.setText(String.valueOf(data.totalBookings));
                    if (activeBookingsKpi != null) activeBookingsKpi.setText(String.valueOf(data.activeBookings));
                    if (completedBookingsKpi != null) completedBookingsKpi.setText(String.valueOf(data.completedBookings));
                    if (revenueLabel != null) revenueLabel.setText("Total Revenue: ₹" + String.format("%.2f", data.totalRevenue) + " | Completed Sessions: " + data.completedBookings);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (revenueLabel != null) revenueLabel.setText("Revenue data unavailable: " + e.getMessage());
                });
            }
        }).start();

        loadRevenueChart();
    }

    private void loadRevenueChart() {
        new Thread(() -> {
            try {
                List<AdminApiService.DailyRevenueData> data = AdminApiService.getRevenueChart();
                Platform.runLater(() -> {
                    revenueChart.getData().clear();
                    XYChart.Series<String, Number> series = new XYChart.Series<>();
                    series.setName("Daily Collections");
                    for (AdminApiService.DailyRevenueData d : data) {
                        series.getData().add(new XYChart.Data<>(d.date, d.revenue));
                    }
                    revenueChart.getData().add(series);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (staffStatusLabel != null) staffStatusLabel.setText("Chart error: " + e.getMessage());
                });
            }
        }).start();
    }

    private void loadStaff() {
        new Thread(() -> {
            try {
                var staffList = AdminApiService.getAllStaff();
                Platform.runLater(() -> staffTable.setItems(FXCollections.observableArrayList(staffList)));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (staffStatusLabel != null) staffStatusLabel.setText("Failed to load staff directory: " + e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    private void handleAddStaff() {
        String name = staffNameField.getText() != null ? staffNameField.getText().trim() : "";
        String email = staffEmailField.getText() != null ? staffEmailField.getText().trim() : "";
        String password = staffPasswordField.getText() != null ? staffPasswordField.getText() : "";
        String phone = staffPhoneField.getText() != null ? staffPhoneField.getText().trim() : "";

        staffStatusLabel.setText("");

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            staffStatusLabel.setStyle("-fx-text-fill: #D64545; -fx-font-weight: bold;");
            staffStatusLabel.setText("Name, email, and password are required.");
            return;
        }

        if (!phone.isEmpty() && !phone.matches("\\d{10}")) {
            staffStatusLabel.setStyle("-fx-text-fill: #D64545; -fx-font-weight: bold;");
            staffStatusLabel.setText("Phone number must be exactly 10 digits.");
            return;
        }

        new Thread(() -> {
            try {
                AdminApiService.createStaff(name, email, password, phone);
                Platform.runLater(() -> {
                    staffStatusLabel.setStyle("-fx-text-fill: #21865B; -fx-font-weight: bold;");
                    staffStatusLabel.setText("Staff operator " + name + " added successfully.");
                    staffNameField.clear();
                    staffEmailField.clear();
                    staffPasswordField.clear();
                    staffPhoneField.clear();
                    loadStaff();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    staffStatusLabel.setStyle("-fx-text-fill: #D64545; -fx-font-weight: bold;");
                    staffStatusLabel.setText("Failed to add staff: " + e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    public void loadBookings() {
        new Thread(() -> {
            try {
                var bookings = AdminApiService.getAllBookings();
                Platform.runLater(() -> allBookings.setAll(bookings));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (staffStatusLabel != null) staffStatusLabel.setText("Failed to load bookings: " + e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    private void handleExportCsv() {
        staffStatusLabel.setText("");

        var bookings = bookingsTable.getItems();
        if (bookings == null || bookings.isEmpty()) {
            staffStatusLabel.setStyle("-fx-text-fill: #D64545; -fx-font-weight: bold;");
            staffStatusLabel.setText("No bookings available to export.");
            return;
        }

        try {
            String fileName = "Bookings_Audit_" + System.currentTimeMillis() + ".csv";
            String userHome = System.getProperty("user.home");
            String filePath = Paths.get(userHome, "Downloads", fileName).toString();

            StringBuilder csv = new StringBuilder();
            csv.append("Booking ID,Slot,Vehicle Number,Status,Entry Time,Exit Time,Fare\n");

            for (BookingApiService.BookingData b : bookings) {
                csv.append(b.id).append(",")
                   .append(b.slotNumber).append(",")
                   .append(b.vehicleNumber).append(",")
                   .append(b.status).append(",")
                   .append(b.entryTime != null ? b.entryTime : "").append(",")
                   .append(b.exitTime != null ? b.exitTime : (b.expectedExitTime != null ? b.expectedExitTime : "")).append(",")
                   .append(b.fare != null ? b.fare : (b.estimatedFare != null ? b.estimatedFare : "0"))
                   .append("\n");
            }

            try (FileWriter writer = new FileWriter(filePath)) {
                writer.write(csv.toString());
            }

            staffStatusLabel.setStyle("-fx-text-fill: #21865B; -fx-font-weight: bold;");
            staffStatusLabel.setText("Exported " + bookings.size() + " records to Downloads: " + fileName);
        } catch (Exception e) {
            staffStatusLabel.setStyle("-fx-text-fill: #D64545; -fx-font-weight: bold;");
            staffStatusLabel.setText("Export failed: " + e.getMessage());
        }
    }

    @FXML
    private void goToDashboard() {
        try { MainApp.switchScene("/fxml/admin_dashboard.fxml"); } catch (Exception e) { e.printStackTrace(); }
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

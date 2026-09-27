package com.parking.client.controller;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.util.Base64;
import java.util.ResourceBundle;

import com.parking.client.MainApp;
import com.parking.client.service.ProfileApiService;
import com.parking.client.util.SessionManager;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

public class ProfileController implements Initializable {

    @FXML private ImageView profileImageView;
    @FXML private Label photoStatusLabel;

    @FXML private TextField nameField;
    @FXML private TextField emailField;
        @FXML
    private Button saveProfileButton;

    @FXML
    private Button changePasswordButton;
    @FXML private TextField phoneField;
    @FXML private TextField addressField;
    @FXML private Label profileErrorLabel;
    @FXML private Label profileSuccessLabel;

    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private Label passwordErrorLabel;
    @FXML private Label passwordSuccessLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        phoneField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                phoneField.setText(newVal.replaceAll("[^\\d]", ""));
            }
            if (phoneField.getText().length() > 10) {
                phoneField.setText(phoneField.getText().substring(0, 10));
            }
        });

        loadProfile();
    }

    private void loadProfile() {
        new Thread(() -> {
            try {
                ProfileApiService.ProfileData data = ProfileApiService.getProfile();
                Platform.runLater(() -> {
                    nameField.setText(data.name);
                    emailField.setText(data.email);
                    phoneField.setText(data.phone != null ? data.phone : "");
                    addressField.setText(data.address != null ? data.address : "");
                    setImageFromBase64(data.photoBase64);
                });
            } catch (Exception e) {
                Platform.runLater(() -> profileErrorLabel.setText("Failed to load profile: " + e.getMessage()));
            }
        }).start();
    }

    private void setImageFromBase64(String base64) {
        if (base64 == null || base64.isEmpty()) {
            profileImageView.setImage(null);
            return;
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            Image image = new Image(new ByteArrayInputStream(bytes));
            profileImageView.setImage(image);
        } catch (Exception e) {
            profileImageView.setImage(null);
        }
    }

    @FXML
    private void handleUploadPhoto() {
        photoStatusLabel.setText("");

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Profile Photo");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );

        File file = fileChooser.showOpenDialog(profileImageView.getScene().getWindow());
        if (file == null) {
            return;
        }

        try {
            byte[] fileBytes = Files.readAllBytes(file.toPath());

            if (fileBytes.length > 2 * 1024 * 1024) {
                photoStatusLabel.setText("Please choose an image under 2MB");
                return;
            }

            String base64 = Base64.getEncoder().encodeToString(fileBytes);

            setImageFromBase64(base64);
            photoStatusLabel.setText("Uploading...");

            new Thread(() -> {
                try {
                    ProfileApiService.updatePhoto(base64);
                    Platform.runLater(() -> photoStatusLabel.setText("Photo updated successfully"));
                } catch (Exception e) {
                    Platform.runLater(() -> photoStatusLabel.setText("Failed to save photo: " + e.getMessage()));
                }
            }).start();

        } catch (Exception e) {
            photoStatusLabel.setText("Failed to read image: " + e.getMessage());
        }
    }

        @FXML
    private void handleUpdateProfile() {
        String name = nameField.getText();
        String phone = phoneField.getText();
        String address = addressField.getText();

        profileErrorLabel.setText("");
        profileSuccessLabel.setText("");

        if (!phone.isEmpty() && phone.length() != 10) {
            profileErrorLabel.setText("Phone number must be exactly 10 digits");
            return;
        }

        saveProfileButton.setDisable(true);
        saveProfileButton.setText("Saving...");

        new Thread(() -> {
            try {
                ProfileApiService.updateProfile(name, phone, address);
                Platform.runLater(() -> {
                    saveProfileButton.setDisable(false);
                    saveProfileButton.setText("Save Changes");
                    profileSuccessLabel.setText("Profile updated successfully");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    saveProfileButton.setDisable(false);
                    saveProfileButton.setText("Save Changes");
                    profileErrorLabel.setText("Update failed: " + e.getMessage());
                });
            }
        }).start();
    }

       @FXML
    private void handleChangePassword() {
        String currentPassword = currentPasswordField.getText();
        String newPassword = newPasswordField.getText();

        passwordErrorLabel.setText("");
        passwordSuccessLabel.setText("");

        if (currentPassword.isEmpty() || newPassword.isEmpty()) {
            passwordErrorLabel.setText("Please fill both password fields");
            return;
        }

        changePasswordButton.setDisable(true);
        changePasswordButton.setText("Changing...");

        new Thread(() -> {
            try {
                ProfileApiService.changePassword(currentPassword, newPassword);
                Platform.runLater(() -> {
                    changePasswordButton.setDisable(false);
                    changePasswordButton.setText("Change Password");
                    passwordSuccessLabel.setText("Password changed successfully");
                    currentPasswordField.clear();
                    newPasswordField.clear();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    changePasswordButton.setDisable(false);
                    changePasswordButton.setText("Change Password");
                    passwordErrorLabel.setText("Failed: " + e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    private void goToDashboard() {
        try { MainApp.switchScene("/fxml/dashboard.fxml"); } catch (Exception e) { profileErrorLabel.setText("Could not navigate"); }
    }

    @FXML
    private void goToProfile() {
        try { MainApp.switchScene("/fxml/profile.fxml"); } catch (Exception e) { profileErrorLabel.setText("Could not navigate"); }
    }

    @FXML
    private void goToVehicles() {
        try { MainApp.switchScene("/fxml/vehicles.fxml"); } catch (Exception e) { profileErrorLabel.setText("Could not navigate"); }
    }

    @FXML
    private void goToBookSlot() {
        try { MainApp.switchScene("/fxml/book_slot.fxml"); } catch (Exception e) { profileErrorLabel.setText("Could not navigate"); }
    }

    @FXML
    private void goToMyBookings() {
        try { MainApp.switchScene("/fxml/my_bookings.fxml"); } catch (Exception e) { profileErrorLabel.setText("Could not navigate"); }
    }

    @FXML
    private void goToPayment() {
        try { MainApp.switchScene("/fxml/payment.fxml"); } catch (Exception e) { profileErrorLabel.setText("Could not navigate"); }
    }

    @FXML
    private void goToNotifications() {
        try { MainApp.switchScene("/fxml/notifications.fxml"); } catch (Exception e) { profileErrorLabel.setText("Could not navigate"); }
    }

    @FXML
    private void goToSettings() {
        try { MainApp.switchScene("/fxml/settings.fxml"); } catch (Exception e) { profileErrorLabel.setText("Could not navigate"); }
    }

    @FXML
    private void handleLogout() {
        SessionManager.clearSession();
        try { MainApp.switchScene("/fxml/login.fxml"); } catch (Exception e) { profileErrorLabel.setText("Could not navigate"); }
    }
}
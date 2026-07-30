package com.parking.client.controller;

import java.net.URL;
import java.util.ResourceBundle;

import com.parking.client.MainApp;
import com.parking.client.service.ProfileApiService;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class ProfileController implements Initializable {

    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private Label profileErrorLabel;
    @FXML private Label profileSuccessLabel;

    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private Label passwordErrorLabel;
    @FXML private Label passwordSuccessLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Restrict phone field to digits, max 10
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
                });
            } catch (Exception e) {
                Platform.runLater(() -> profileErrorLabel.setText("Failed to load profile: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleUpdateProfile() {
        String name = nameField.getText();
        String phone = phoneField.getText();

        profileErrorLabel.setText("");
        profileSuccessLabel.setText("");

        if (!phone.isEmpty() && phone.length() != 10) {
            profileErrorLabel.setText("Phone number must be exactly 10 digits");
            return;
        }

        new Thread(() -> {
            try {
                ProfileApiService.updateProfile(name, phone);
                Platform.runLater(() -> profileSuccessLabel.setText("Profile updated successfully"));
            } catch (Exception e) {
                Platform.runLater(() -> profileErrorLabel.setText("Update failed: " + e.getMessage()));
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

        new Thread(() -> {
            try {
                ProfileApiService.changePassword(currentPassword, newPassword);
                Platform.runLater(() -> {
                    passwordSuccessLabel.setText("Password changed successfully");
                    currentPasswordField.clear();
                    newPasswordField.clear();
                });
            } catch (Exception e) {
                Platform.runLater(() -> passwordErrorLabel.setText("Failed: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void goToDashboard() {
        try {
            MainApp.switchScene("/fxml/dashboard.fxml");
        } catch (Exception e) {
            profileErrorLabel.setText("Could not return to dashboard");
        }
    }
}
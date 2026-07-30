package com.parking.client.controller;

import com.parking.client.MainApp;
import com.parking.client.service.AuthApiService;
import com.parking.client.util.ResetFlowState;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;

public class ResetPasswordController {

    @FXML
    private PasswordField newPasswordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label errorLabel;

    @FXML
    private void handleResetPassword() {
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String email = ResetFlowState.getEmail();

        if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
            errorLabel.setText("Please fill both fields");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            errorLabel.setText("Passwords do not match");
            return;
        }

        if (email == null) {
            errorLabel.setText("Session expired. Please restart forgot password process");
            return;
        }

        errorLabel.setText("Resetting password...");

        new Thread(() -> {
            try {
                AuthApiService.resetPassword(email, newPassword);
                ResetFlowState.clear();

                Platform.runLater(() -> {
                    try {
                        MainApp.switchScene("/fxml/login.fxml");
                    } catch (Exception e) {
                        errorLabel.setText("Password reset but couldn't return to login");
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() ->
                        errorLabel.setText("Failed: " + e.getMessage())
                );
            }
        }).start();
    }
}   
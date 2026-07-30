package com.parking.client.controller;

import com.parking.client.MainApp;
import com.parking.client.service.AuthApiService;
import com.parking.client.util.ResetFlowState;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class ForgotPasswordController {

    @FXML
    private TextField emailField;

    @FXML
    private Label errorLabel;

    @FXML
    private void handleSendOtp() {
        String email = emailField.getText();

        if (email.isEmpty()) {
            errorLabel.setText("Please enter your email");
            return;
        }

        errorLabel.setText("Sending OTP...");

        new Thread(() -> {
            try {
                AuthApiService.forgotPassword(email);
                ResetFlowState.setEmail(email);

                Platform.runLater(() -> {
                    try {
                        MainApp.switchScene("/fxml/verify_otp.fxml");
                    } catch (Exception e) {
                        errorLabel.setText("Could not open OTP screen");
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() ->
                        errorLabel.setText("Failed: " + e.getMessage())
                );
            }
        }).start();
    }

    @FXML
    private void goToLogin() {
        try {
            MainApp.switchScene("/fxml/login.fxml");
        } catch (Exception e) {
            errorLabel.setText("Login screen not available");
        }
    }
}
package com.parking.client.controller;

import com.parking.client.MainApp;
import com.parking.client.service.AuthApiService;
import com.parking.client.util.ResetFlowState;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class VerifyOtpController {

    @FXML
    private TextField otpField;

    @FXML
    private Label errorLabel;

    @FXML
    private Label infoLabel;

    @FXML
    private void handleVerifyOtp() {
        String otp = otpField.getText();
        String email = ResetFlowState.getEmail();

        if (otp.isEmpty()) {
            errorLabel.setText("Please enter the OTP");
            return;
        }

        if (email == null) {
            errorLabel.setText("Session expired. Please restart forgot password process");
            return;
        }

        errorLabel.setText("Verifying...");

        new Thread(() -> {
            try {
                AuthApiService.verifyOtp(email, otp);

                Platform.runLater(() -> {
                    try {
                        MainApp.switchScene("/fxml/reset_password.fxml");
                    } catch (Exception e) {
                        errorLabel.setText("Could not open reset password screen");
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
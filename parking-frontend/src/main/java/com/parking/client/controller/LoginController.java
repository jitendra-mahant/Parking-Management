package com.parking.client.controller;

import com.parking.client.MainApp;
import com.parking.client.service.AuthApiService;
import com.parking.client.util.SessionManager;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    @FXML
    private void handleLogin() {
        String email = emailField.getText();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please enter both email and password");
            return;
        }

        errorLabel.setText("Logging in...");

        new Thread(() -> {
            try {
                AuthApiService.AuthResult result = AuthApiService.login(email, password);

                SessionManager.setSession(result.token, result.name, result.email, result.role);

                javafx.application.Platform.runLater(() -> {
                    try {
                        // For now, all roles go to a generic dashboard - we'll split by role next
                        MainApp.switchScene("/fxml/dashboard.fxml");
                    } catch (Exception e) {
                        errorLabel.setText("Login succeeded but dashboard not ready yet");
                    }
                });

            } catch (Exception e) {
                javafx.application.Platform.runLater(() ->
                        errorLabel.setText("Login failed: " + e.getMessage())
                );
            }
        }).start();
    }

    @FXML
    private void goToSignup() {
        try {
            MainApp.switchScene("/fxml/signup.fxml");
        } catch (Exception e) {
            errorLabel.setText("Signup screen not ready yet");
        }
    }
}
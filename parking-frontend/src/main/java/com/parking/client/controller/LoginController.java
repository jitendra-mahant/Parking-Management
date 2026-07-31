package com.parking.client.controller;

import com.parking.client.MainApp;
import com.parking.client.service.AuthApiService;
import com.parking.client.util.SessionManager;

import javafx.application.Platform;
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

                Platform.runLater(() -> {
                    try {
                        if (result.role.equals("staff")) {
                            MainApp.switchScene("/fxml/staff_dashboard.fxml");
                        } else if (result.role.equals("admin")) {
                            MainApp.switchScene("/fxml/admin_dashboard.fxml");
                        } else {
                            MainApp.switchScene("/fxml/dashboard.fxml");
                        }
                    } catch (Exception e) {
                        errorLabel.setText("Login succeeded but dashboard failed to load: " + e.getMessage());
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() ->
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

    @FXML
    private void goToForgotPassword() {
        try {
            MainApp.switchScene("/fxml/forgot_password.fxml");
        } catch (Exception e) {
            errorLabel.setText("Forgot password screen not available");
        }
    }
}
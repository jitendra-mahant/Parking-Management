package com.parking.client.controller;

import com.parking.client.MainApp;
import com.parking.client.service.AuthApiService;
import com.parking.client.util.SessionManager;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.shape.SVGPath;

public class LoginController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField passwordVisibleField;

    @FXML
    private Button togglePasswordBtn;

    @FXML
    private SVGPath eyeIcon;

    @FXML
    private Label errorLabel;

    @FXML
    private Button loginButton;

    private boolean isPasswordVisible = false;

    // Standard SVG Eye Path definitions (show / hide)
    private static final String EYE_SHOW = "M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z M12 9a3 3 0 1 0 0 6 3 3 0 0 0 0-6z";
    private static final String EYE_HIDE = "M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24 M1 1l22 22";

    @FXML
    public void initialize() {
        if (passwordField != null && passwordVisibleField != null) {
            passwordField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!isPasswordVisible && newVal != null && !newVal.equals(passwordVisibleField.getText())) {
                    passwordVisibleField.setText(newVal);
                }
            });
            passwordVisibleField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (isPasswordVisible && newVal != null && !newVal.equals(passwordField.getText())) {
                    passwordField.setText(newVal);
                }
            });
        }
    }

    @FXML
    private void togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible;
        if (isPasswordVisible) {
            passwordVisibleField.setText(passwordField.getText());
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            passwordVisibleField.setVisible(true);
            passwordVisibleField.setManaged(true);
            if (eyeIcon != null) {
                eyeIcon.setContent(EYE_HIDE);
                eyeIcon.setStyle("-fx-fill: transparent; -fx-stroke: #0F9D8A; -fx-stroke-width: 1.6; -fx-stroke-line-cap: round; -fx-stroke-line-join: round;");
            }
            passwordVisibleField.requestFocus();
            passwordVisibleField.positionCaret(passwordVisibleField.getText().length());
        } else {
            passwordField.setText(passwordVisibleField.getText());
            passwordVisibleField.setVisible(false);
            passwordVisibleField.setManaged(false);
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            if (eyeIcon != null) {
                eyeIcon.setContent(EYE_SHOW);
                eyeIcon.setStyle("-fx-fill: transparent; -fx-stroke: #64748B; -fx-stroke-width: 1.6; -fx-stroke-line-cap: round; -fx-stroke-line-join: round;");
            }
            passwordField.requestFocus();
            passwordField.positionCaret(passwordField.getText().length());
        }
    }

    @FXML
    private void handleLogin() {
        String email = emailField.getText() != null ? emailField.getText().trim() : "";
        String password = isPasswordVisible
                ? (passwordVisibleField.getText() != null ? passwordVisibleField.getText() : "")
                : (passwordField.getText() != null ? passwordField.getText() : "");

        if (email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please enter both email and password");
            return;
        }

        errorLabel.setText("");
        loginButton.setDisable(true);
        loginButton.setText("Logging in...");

        new Thread(() -> {
            try {
                AuthApiService.AuthResult result = AuthApiService.login(email, password);

                SessionManager.setSession(result.token, result.name, result.email, result.role);

                Platform.runLater(() -> {
                    try {
                        if ("staff".equalsIgnoreCase(result.role)) {
                            MainApp.switchScene("/fxml/staff_dashboard.fxml");
                        } else if ("admin".equalsIgnoreCase(result.role)) {
                            MainApp.switchScene("/fxml/admin_dashboard.fxml");
                        } else {
                            MainApp.switchScene("/fxml/dashboard.fxml");
                        }
                    } catch (Exception e) {
                        loginButton.setDisable(false);
                        loginButton.setText("Sign In to Dashboard");
                        errorLabel.setText("Login succeeded but dashboard failed to load: " + e.getMessage());
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    loginButton.setDisable(false);
                    loginButton.setText("Sign In to Dashboard");
                    errorLabel.setText("Login failed: " + e.getMessage());
                });
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

package com.parking.client.controller;

import java.net.URL;
import java.util.ResourceBundle;

import com.parking.client.MainApp;
import com.parking.client.service.AuthApiService;
import com.parking.client.util.SessionManager;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class SignupController implements Initializable {

    @FXML
    private TextField nameField;

    @FXML
    private TextField emailField;
    
        @FXML
    private Button signupButton;

    @FXML
    private TextField phoneField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label errorLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Restrict phone field: numbers only, max 10 digits
        phoneField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                phoneField.setText(newValue.replaceAll("[^\\d]", ""));
            }
            if (phoneField.getText().length() > 10) {
                phoneField.setText(phoneField.getText().substring(0, 10));
            }
        });
    }

    @FXML
    private void handleSignup() {
        String name = nameField.getText();
        String email = emailField.getText();
        String phone = phoneField.getText();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please fill all required fields");
            return;
        }

        if (!phone.isEmpty() && phone.length() != 10) {
            errorLabel.setText("Phone number must be exactly 10 digits");
            return;
        }

        if (!password.equals(confirmPassword)) {
            errorLabel.setText("Passwords do not match");
            return;
        }

               errorLabel.setText("");
        signupButton.setDisable(true);
        signupButton.setText("Creating account...");

        new Thread(() -> {
            try {
                AuthApiService.AuthResult result = AuthApiService.signup(name, email, password, phone);

                SessionManager.setSession(result.token, result.name, result.email, result.role);

                Platform.runLater(() -> {
                    try {
                        MainApp.switchScene("/fxml/dashboard.fxml");
                    } catch (Exception e) {
                        signupButton.setDisable(false);
                        signupButton.setText("Sign Up");
                        errorLabel.setText("Signup succeeded but dashboard failed to load");
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    signupButton.setDisable(false);
                    signupButton.setText("Sign Up");
                    errorLabel.setText("Signup failed: " + e.getMessage());
                });
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
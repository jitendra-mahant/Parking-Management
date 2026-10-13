package com.parking.client;

import com.parking.client.util.ThemeManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class MainApp extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        primaryStage.setTitle("Parking Management System");

        // Size the window to fit the user's screen (never taller/wider than the visible desktop area)
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        primaryStage.setWidth(Math.min(1200, bounds.getWidth() * 0.95));
        primaryStage.setHeight(Math.min(800, bounds.getHeight() * 0.95));
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(560);
        primaryStage.centerOnScreen();

        switchScene("/fxml/login.fxml");

        primaryStage.show();
    }

    public static void switchScene(String fxmlPath) throws Exception {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource(fxmlPath));
        Parent root = loader.load();

        if (root instanceof BorderPane) {
            // Dashboard-style pages: keep the sidebar fixed, make only the content area scroll
            BorderPane borderPane = (BorderPane) root;
            Node center = borderPane.getCenter();
            if (center != null) {
                borderPane.setCenter(null);
                ScrollPane scroll = new ScrollPane(center);
                scroll.setFitToWidth(true);
                scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                scroll.getStyleClass().add("content-scroll");
                borderPane.setCenter(scroll);
            }
        } else if (root instanceof StackPane) {
            // Login / Signup / OTP screens: card stays centered, scrolls only if the window is too short
            ScrollPane scroll = new ScrollPane(root);
            scroll.setFitToWidth(true);
            scroll.setFitToHeight(true);
            scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scroll.setStyle("-fx-background-color: transparent; -fx-background-insets: 0; -fx-padding: 0;");
            root = scroll;
        }

        Scene scene = new Scene(root);
        scene.getStylesheets().add(MainApp.class.getResource("/css/styles.css").toExternalForm());

        if (ThemeManager.isDarkMode()) {
            root.getStyleClass().add("dark-root");
        }

        primaryStage.setScene(scene);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
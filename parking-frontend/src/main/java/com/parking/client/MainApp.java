package com.parking.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        primaryStage.setTitle("Parking Management System");

        primaryStage.setWidth(1200);
        primaryStage.setHeight(800);
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(650);

        switchScene("/fxml/login.fxml");

        primaryStage.show();
    }

    // Utility method to switch screens (login -> dashboard etc.)
    public static void switchScene(String fxmlPath) throws Exception {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource(fxmlPath));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(MainApp.class.getResource("/css/styles.css").toExternalForm());

        if (com.parking.client.util.ThemeManager.isDarkMode()) {
            root.getStyleClass().add("dark-root");
        }

        primaryStage.setScene(scene);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
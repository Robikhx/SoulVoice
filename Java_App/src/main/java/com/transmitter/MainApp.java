package com.transmitter;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        System.out.println("MainApp.start() called");
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/transmitter/main.fxml")
            );

            Scene scene = new Scene(loader.load(), 1200, 800);
            primaryStage.setTitle("Speak Easy");
            scene.getStylesheets().add(
                getClass().getResource("/com/transmitter/style.css").toExternalForm()
            );
            primaryStage.setScene(scene);
            primaryStage.show();

            MainController controller = loader.getController();
            primaryStage.setOnCloseRequest(event -> {
                controller.cleanup();
            });
        } catch (Throwable e) {
            // If FXML loading fails, show error window
            System.err.println("Error loading application: " + e.getMessage());
            e.printStackTrace();
            
            VBox errorBox = new VBox();
            errorBox.setStyle("-fx-padding: 20; -fx-spacing: 10;");
            Label errorLabel = new Label("Error loading application:\n" + e.getClass().getSimpleName() + ": " + e.getMessage());
            errorBox.getChildren().add(errorLabel);
            
            Scene errorScene = new Scene(errorBox, 400, 200);
            primaryStage.setTitle("Error");
            primaryStage.setScene(errorScene);
            primaryStage.show();
            
            System.out.println("Error window shown to user");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
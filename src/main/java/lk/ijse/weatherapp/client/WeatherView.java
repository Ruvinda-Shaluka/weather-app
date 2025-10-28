package lk.ijse.weatherapp.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lk.ijse.weatherapp.controller.WeatherController;

import java.io.IOException;

public class WeatherView extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/weather-view.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 1200, 800);

            primaryStage.setTitle("Real-Time Weather Dashboard");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1000);
            primaryStage.setMinHeight(700);

            primaryStage.setOnCloseRequest(event -> {
                WeatherController controller = loader.getController();
                if (controller != null) {
                    controller.getWeatherService().disconnect();
                }
            });

            primaryStage.show();

        } catch (IOException e) {
            System.err.println("Error loading FXML: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
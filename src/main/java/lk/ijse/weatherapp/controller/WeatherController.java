package lk.ijse.weatherapp.controller;

import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lk.ijse.weatherapp.client.WeatherService;
import lk.ijse.weatherapp.model.WeatherData;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.ResourceBundle;

public class WeatherController implements Initializable {

    @FXML private VBox root;
    @FXML private Label headerLabel;
    @FXML private HBox connectionPanel;
    @FXML private Label statusLabel;
    @FXML private Label tempLabel;
    @FXML private Label humidityLabel;
    @FXML private Label windLabel;
    @FXML private Label conditionLabel;
    @FXML private TextField ipField;
    @FXML private TextField portField;
    @FXML private Button connectButton;
    @FXML private Button disconnectButton;
    @FXML private LineChart<Number, Number> tempChart;
    @FXML private LineChart<Number, Number> humidityChart;
    @FXML private LineChart<Number, Number> windChart;
    @FXML private NumberAxis tempXAxis, tempYAxis, humidityXAxis, humidityYAxis, windXAxis, windYAxis;

    private WeatherService weatherService;

    // Data for charts
    private final ObservableList<XYChart.Data<Number, Number>> tempData = FXCollections.observableArrayList();
    private final ObservableList<XYChart.Data<Number, Number>> humidityData = FXCollections.observableArrayList();
    private final ObservableList<XYChart.Data<Number, Number>> windData = FXCollections.observableArrayList();

    private XYChart.Series<Number, Number> tempSeries;
    private XYChart.Series<Number, Number> humiditySeries;
    private XYChart.Series<Number, Number> windSeries;

    private int dataPointCount = 0;
    private static final int MAX_DATA_POINTS = 20;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.weatherService = new WeatherService();
        setupWeatherServiceListener();
        initializeCharts();
        setDefaultValues();
    }

    private void initializeCharts() {
        // Initialize chart series
        tempSeries = new XYChart.Series<>();
        tempSeries.setName("Temperature");
        tempSeries.setData(tempData);
        tempChart.getData().add(tempSeries);
        tempChart.setLegendVisible(false);

        humiditySeries = new XYChart.Series<>();
        humiditySeries.setName("Humidity");
        humiditySeries.setData(humidityData);
        humidityChart.getData().add(humiditySeries);
        humidityChart.setLegendVisible(false);

        windSeries = new XYChart.Series<>();
        windSeries.setName("Wind Speed");
        windSeries.setData(windData);
        windChart.getData().add(windSeries);
        windChart.setLegendVisible(false);
    }

    private void setDefaultValues() {
        ipField.setText("localhost");
        portField.setText("8080");
        disconnectButton.setDisable(true);
    }

    private void setupWeatherServiceListener() {
        weatherService.setWeatherDataListener(new WeatherService.WeatherDataListener() {
            @Override
            public void onWeatherDataReceived(WeatherData data) {
                Platform.runLater(() -> updateWeatherData(data));
            }

            @Override
            public void onConnectionStatusChanged(boolean connected, String message) {
                Platform.runLater(() -> updateStatus(message, connected ? "green" : "red"));
                Platform.runLater(() -> setConnectionControls(connected));
            }
        });
    }

    @FXML
    private void handleConnect() {
        try {
            String ip = ipField.getText();
            int port = Integer.parseInt(portField.getText());

            new Thread(() -> {
                boolean success = weatherService.connect(ip, port);
                if (!success) {
                    Platform.runLater(() -> updateStatus("Connection failed", "red"));
                }
            }).start();

        } catch (NumberFormatException e) {
            updateStatus("Invalid port number", "red");
        }
    }

    @FXML
    private void handleDisconnect() {
        weatherService.disconnect();
    }

    private void updateWeatherData(WeatherData weatherData) {
        // Update dashboard
        tempLabel.setText(String.format("%.1f °C", weatherData.getTemperature()));
        humidityLabel.setText(String.format("%.1f %%", weatherData.getHumidity()));
        windLabel.setText(String.format("%.1f km/h", weatherData.getWindSpeed()));
        conditionLabel.setText(weatherData.getCondition());

        // Update charts
        updateChart(tempData, weatherData.getTemperature());
        updateChart(humidityData, weatherData.getHumidity());
        updateChart(windData, weatherData.getWindSpeed());

        // Update status with timestamp
        updateStatus("Last update: " + weatherData.getFormattedTime(), "blue");
    }

    private void updateChart(ObservableList<XYChart.Data<Number, Number>> data, double value) {
        data.add(new XYChart.Data<>(dataPointCount, value));

        // Limit data points for better visualization
        if (data.size() > MAX_DATA_POINTS) {
            data.remove(0);
        }

        dataPointCount++;
    }

    public void updateStatus(String message, String color) {
        statusLabel.setText(message);

        // Remove existing status classes
        statusLabel.getStyleClass().removeAll("status-connected", "status-disconnected", "status-update");

        // Add appropriate class based on color
        switch (color) {
            case "green":
                statusLabel.getStyleClass().add("status-connected");
                break;
            case "red":
                statusLabel.getStyleClass().add("status-disconnected");
                break;
            case "blue":
                statusLabel.getStyleClass().add("status-update");
                break;
        }
    }

    public void setConnectionControls(boolean connected) {
        connectButton.setDisable(connected);
        disconnectButton.setDisable(!connected);
        ipField.setDisable(connected);
        portField.setDisable(connected);

        if (!connected) {
            // Reset dashboard when disconnected
            resetDashboard();
        }
    }

    private void resetDashboard() {
        tempLabel.setText("-- °C");
        humidityLabel.setText("-- %");
        windLabel.setText("-- km/h");
        conditionLabel.setText("--");

        tempData.clear();
        humidityData.clear();
        windData.clear();
        dataPointCount = 0;
    }

    public WeatherService getWeatherService() {
        return weatherService;
    }
}
package lk.ijse.weatherapp.controller;

import lk.ijse.weatherapp.client.WeatherService;
import lk.ijse.weatherapp.client.util.LocationDetector;
import lk.ijse.weatherapp.model.WeatherData;
import lk.ijse.weatherapp.model.Location;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

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
    @FXML private Label locationLabel;
    @FXML private Label pressureLabel;
    @FXML private Label feelsLikeLabel;
    @FXML private Label uvLabel;
    @FXML private Label visibilityLabel;
    // Remove this line: @FXML private Label cloudinessLabel;
    @FXML private TextField ipField;
    @FXML private TextField portField;
    @FXML private Button connectButton;
    @FXML private Button disconnectButton;
    @FXML private Button detectLocationButton;
    @FXML private LineChart<Number, Number> tempChart;
    @FXML private LineChart<Number, Number> humidityChart;
    @FXML private LineChart<Number, Number> windChart;

    private WeatherService weatherService;
    private Location clientLocation;

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
        autoDetectLocation();
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

    private void autoDetectLocation() {
        updateStatus("Detecting your location...", "blue");

        LocationDetector.detectLocation(new LocationDetector.LocationCallback() {
            @Override
            public void onLocationDetected(Location location) {
                clientLocation = location;
                Platform.runLater(() -> {
                    locationLabel.setText(location.getCity() + ", " + location.getCountry());
                    updateStatus("Location detected: " + location.getCity(), "green");
                });
            }

            @Override
            public void onLocationError(String error) {
                Platform.runLater(() -> {
                    // Use default location
                    clientLocation = new Location("Colombo", "Sri Lanka", 6.9271, 79.8612, "Asia/Colombo");
                    locationLabel.setText("Colombo, Sri Lanka (Default)");
                    updateStatus("Using default location - " + error, "orange");
                });
            }
        });
    }

    @FXML
    private void handleDetectLocation() {
        autoDetectLocation();
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
        if (clientLocation == null) {
            updateStatus("Please wait for location detection or set location manually", "red");
            return;
        }

        try {
            String ip = ipField.getText();
            int port = Integer.parseInt(portField.getText());

            // Set client location before connecting
            weatherService.setClientLocation(clientLocation);

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
        // Update dashboard with enhanced data
        String weatherIcon = weatherData.getWeatherIcon();
        tempLabel.setText(String.format("%s %.1f °C", weatherIcon, weatherData.getTemperature()));
        humidityLabel.setText(String.format("💧 %.1f %%", weatherData.getHumidity()));
        windLabel.setText(String.format("💨 %.1f km/h", weatherData.getWindSpeed()));
        conditionLabel.setText(weatherData.getDescription());

        // Update location and additional info
        locationLabel.setText(weatherData.getLocation().getCity() + ", " + weatherData.getLocation().getCountry());
        pressureLabel.setText(String.format("🔄 %.1f hPa", weatherData.getPressure()));
        feelsLikeLabel.setText(String.format("🌡️ %.1f °C", weatherData.getFeelsLike()));
        uvLabel.setText(String.format("☀️ %d", weatherData.getUvIndex()));
        visibilityLabel.setText(String.format("👁️ %d km", weatherData.getVisibility()));
        // Remove this line: cloudinessLabel.setText(String.format("☁️ %d%%", weatherData.getCloudiness()));

        // Update charts
        updateChart(tempData, weatherData.getTemperature());
        updateChart(humidityData, weatherData.getHumidity());
        updateChart(windData, weatherData.getWindSpeed());

        // Update status with timestamp and location
        updateStatus("Live from " + weatherData.getLocation().getCity() + " | " + weatherData.getFormattedTime(), "blue");
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
        statusLabel.getStyleClass().removeAll("status-connected", "status-disconnected", "status-update", "status-warning");

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
            case "orange":
                statusLabel.getStyleClass().add("status-warning");
                break;
        }
    }

    public void setConnectionControls(boolean connected) {
        connectButton.setDisable(connected);
        disconnectButton.setDisable(!connected);
        ipField.setDisable(connected);
        portField.setDisable(connected);
        detectLocationButton.setDisable(connected);

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
        pressureLabel.setText("-- hPa");
        feelsLikeLabel.setText("-- °C");
        uvLabel.setText("--");
        visibilityLabel.setText("-- km");
        // Remove this line: cloudinessLabel.setText("--%");

        // Clear charts
        tempData.clear();
        humidityData.clear();
        windData.clear();
        dataPointCount = 0;
    }

    // Getter for FXML loader
    public WeatherService getWeatherService() {
        return weatherService;
    }
}
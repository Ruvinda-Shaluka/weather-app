package lk.ijse.weatherapp.server;

import lk.ijse.weatherapp.model.WeatherData;
import java.util.Random;

public class WeatherSimulator {
    private final Random random;
    private double currentTemp;
    private double currentHumidity;
    private double currentWindSpeed;
    
    // Weather conditions
    private final String[] CONDITIONS = {"Sunny", "Cloudy", "Rainy", "Partly Cloudy", "Windy", "Stormy"};
    
    public WeatherSimulator() {
        this.random = new Random();
        // Initial realistic values
        this.currentTemp = 20 + random.nextDouble() * 15; // 20-35°C
        this.currentHumidity = 40 + random.nextDouble() * 50; // 40-90%
        this.currentWindSpeed = random.nextDouble() * 30; // 0-30 km/h
    }
    
    public WeatherData generateWeatherData() {
        // Simulate gradual weather changes
        currentTemp += (random.nextDouble() - 0.5) * 2; // Change by ±1°C
        currentTemp = Math.max(-10, Math.min(45, currentTemp)); // Keep within realistic range
        
        currentHumidity += (random.nextDouble() - 0.5) * 10;
        currentHumidity = Math.max(0, Math.min(100, currentHumidity));
        
        currentWindSpeed += (random.nextDouble() - 0.5) * 5;
        currentWindSpeed = Math.max(0, Math.min(100, currentWindSpeed));
        
        String condition = CONDITIONS[random.nextInt(CONDITIONS.length)];
        
        return new WeatherData(
            Math.round(currentTemp * 10) / 10.0,
            Math.round(currentHumidity * 10) / 10.0,
            Math.round(currentWindSpeed * 10) / 10.0,
            condition
        );
    }
}
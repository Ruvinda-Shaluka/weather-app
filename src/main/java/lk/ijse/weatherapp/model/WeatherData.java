package lk.ijse.weatherapp.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class WeatherData implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private double temperature;
    private double humidity;
    private double windSpeed;
    private String condition;
    private LocalDateTime timestamp;
    
    public WeatherData(double temperature, double humidity, double windSpeed, String condition) {
        this.temperature = temperature;
        this.humidity = humidity;
        this.windSpeed = windSpeed;
        this.condition = condition;
        this.timestamp = LocalDateTime.now();
    }


    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
    
    public double getHumidity() { return humidity; }
    public void setHumidity(double humidity) { this.humidity = humidity; }
    
    public double getWindSpeed() { return windSpeed; }
    public void setWindSpeed(double windSpeed) { this.windSpeed = windSpeed; }
    
    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public String getFormattedTime() {
        return timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
    
    @Override
    public String toString() {
        return String.format("WeatherData{Temp=%.1f°C, Humidity=%.1f%%, Wind=%.1fkm/h, Condition=%s, Time=%s}", 
            temperature, humidity, windSpeed, condition, getFormattedTime());
    }
}
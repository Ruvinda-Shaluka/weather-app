package lk.ijse.weatherapp.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class WeatherData implements Serializable {
    private static final long serialVersionUID = 1L;

    private Location location;
    private double temperature;
    private double humidity;
    private double windSpeed;
    private String condition;
    private String description;
    private double pressure;
    private double feelsLike;
    private int uvIndex;
    private int visibility;
    private int cloudiness;
    private LocalDateTime timestamp;

    public WeatherData() {}

    public WeatherData(Location location, double temperature, double humidity, double windSpeed,
                       String condition, String description, double pressure, double feelsLike,
                       int uvIndex, int visibility, int cloudiness) {
        this.location = location;
        this.temperature = temperature;
        this.humidity = humidity;
        this.windSpeed = windSpeed;
        this.condition = condition;
        this.description = description;
        this.pressure = pressure;
        this.feelsLike = feelsLike;
        this.uvIndex = uvIndex;
        this.visibility = visibility;
        this.cloudiness = cloudiness;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }

    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }

    public double getHumidity() { return humidity; }
    public void setHumidity(double humidity) { this.humidity = humidity; }

    public double getWindSpeed() { return windSpeed; }
    public void setWindSpeed(double windSpeed) { this.windSpeed = windSpeed; }

    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getPressure() { return pressure; }
    public void setPressure(double pressure) { this.pressure = pressure; }

    public double getFeelsLike() { return feelsLike; }
    public void setFeelsLike(double feelsLike) { this.feelsLike = feelsLike; }

    public int getUvIndex() { return uvIndex; }
    public void setUvIndex(int uvIndex) { this.uvIndex = uvIndex; }

    public int getVisibility() { return visibility; }
    public void setVisibility(int visibility) { this.visibility = visibility; }

    public int getCloudiness() { return cloudiness; }
    public void setCloudiness(int cloudiness) { this.cloudiness = cloudiness; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getFormattedTime() {
        return timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    public String getWeatherIcon() {
        if (condition == null) return "🌤️";

        switch (condition.toLowerCase()) {
            case "clear": return "☀️";
            case "clouds": return cloudiness > 70 ? "☁️" : "⛅";
            case "rain": return "🌧️";
            case "drizzle": return "🌦️";
            case "thunderstorm": return "⛈️";
            case "snow": return "❄️";
            case "mist": case "fog": return "🌫️";
            default: return "🌤️";
        }
    }

    @Override
    public String toString() {
        return String.format("WeatherData{Location=%s, Temp=%.1f°C, Humidity=%.1f%%, Wind=%.1fkm/h, Condition=%s}",
                location, temperature, humidity, windSpeed, condition);
    }
}
package lk.ijse.weatherapp.server;

import lk.ijse.weatherapp.model.WeatherData;
import lk.ijse.weatherapp.model.Location;

public interface WeatherService {
    WeatherData getCurrentWeather(Location location);
    WeatherData getCurrentWeather(double latitude, double longitude);
    boolean isServiceAvailable();
}
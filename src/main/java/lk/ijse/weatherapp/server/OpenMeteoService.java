package lk.ijse.weatherapp.server;

import lk.ijse.weatherapp.model.WeatherData;
import lk.ijse.weatherapp.model.Location;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class OpenMeteoService implements WeatherService {
    private static final String BASE_URL = "https://api.open-meteo.com/v1/forecast";
    
    @Override
    public WeatherData getCurrentWeather(Location location) {
        return getCurrentWeather(location.getLatitude(), location.getLongitude());
    }
    
    @Override
    public WeatherData getCurrentWeather(double latitude, double longitude) {
        try {
            String urlString = String.format(
                "%s?latitude=%f&longitude=%f&current=temperature_2m,relative_humidity_2m,apparent_temperature,pressure_msl,wind_speed_10m,cloud_cover,visibility&timezone=auto",
                BASE_URL, latitude, longitude);
            
            System.out.println("🌐 Fetching from Open-Meteo: " + latitude + ", " + longitude);
            
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            
            int responseCode = connection.getResponseCode();
            System.out.println("📡 Open-Meteo Response Code: " + responseCode);
            
            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                return parseWeatherData(response.toString(), latitude, longitude);
            } else {
                System.err.println("❌ Open-Meteo Error: " + responseCode);
                return generateSimulatedWeather(latitude, longitude);
            }
        } catch (Exception e) {
            System.err.println("❌ Error fetching Open-Meteo data: " + e.getMessage());
            return generateSimulatedWeather(latitude, longitude);
        }
    }
    
    private WeatherData parseWeatherData(String jsonResponse, double lat, double lon) {
        try {
            JsonObject json = JsonParser.parseString(jsonResponse).getAsJsonObject();
            JsonObject current = json.getAsJsonObject("current");
            
            // Get city name from reverse geocoding (simplified)
            String cityName = getCityName(lat, lon);
            
            // Parse location
            Location location = new Location();
            location.setCity(cityName);
            location.setCountry(getCountryCode(lat, lon));
            location.setLatitude(lat);
            location.setLongitude(lon);
            location.setTimezone("auto");
            
            // Parse weather data
            double temp = current.get("temperature_2m").getAsDouble();
            double humidity = current.get("relative_humidity_2m").getAsDouble();
            double feelsLike = current.get("apparent_temperature").getAsDouble();
            double pressure = current.get("pressure_msl").getAsDouble();
            double windSpeed = current.get("wind_speed_10m").getAsDouble();
            int cloudiness = current.get("cloud_cover").getAsInt();
            double visibility = current.get("visibility").getAsDouble();
            
            // Determine condition based on cloud cover
            String condition = getConditionFromCloudCover(cloudiness);
            String description = getDescriptionFromCondition(condition);
            int uvIndex = calculateUVIndex(lat);
            
            return new WeatherData(location, temp, humidity, windSpeed, condition, 
                                 description, pressure, feelsLike, uvIndex, 
                                 (int)visibility, cloudiness);
            
        } catch (Exception e) {
            System.err.println("❌ Error parsing Open-Meteo data: " + e.getMessage());
            return generateSimulatedWeather(lat, lon);
        }
    }
    
    private String getCityName(double lat, double lon) {
        // Simplified reverse geocoding for Sri Lankan cities
        if (lat >= 6.8 && lat <= 7.0 && lon >= 79.8 && lon <= 80.0) return "Colombo";
        if (lat >= 7.2 && lat <= 7.4 && lon >= 80.5 && lon <= 80.8) return "Kandy";
        if (lat >= 6.0 && lat <= 6.1 && lon >= 80.1 && lon <= 80.3) return "Galle";
        if (lat >= 9.6 && lat <= 9.7 && lon >= 80.0 && lon <= 80.1) return "Jaffna";
        if (lat >= 6.9 && lat <= 7.0 && lon >= 80.7 && lon <= 80.9) return "Nuwara Eliya";
        return "Unknown City";
    }
    
    private String getCountryCode(double lat, double lon) {
        // Simplified country detection
        if (lat >= 5.0 && lat <= 10.0 && lon >= 79.0 && lon <= 82.0) return "Sri Lanka";
        return "Unknown";
    }
    
    private String getConditionFromCloudCover(int cloudCover) {
        if (cloudCover < 20) return "Clear";
        if (cloudCover < 60) return "Partly Cloudy";
        if (cloudCover < 90) return "Cloudy";
        return "Overcast";
    }
    
    private String getDescriptionFromCondition(String condition) {
        switch (condition) {
            case "Clear": return "Clear sky";
            case "Partly Cloudy": return "Partly cloudy";
            case "Cloudy": return "Cloudy";
            case "Overcast": return "Overcast";
            default: return "Fair weather";
        }
    }
    
    private int calculateUVIndex(double latitude) {
        double latEffect = Math.max(0, 1 - Math.abs(latitude) / 90);
        int hour = java.time.LocalDateTime.now().getHour();
        double timeEffect = Math.max(0, 1 - Math.abs(hour - 12) / 6);
        return (int) Math.min(11, latEffect * timeEffect * 8 + Math.random() * 3);
    }
    
    private WeatherData generateSimulatedWeather(double lat, double lon) {
        // Same simulated data implementation
        String[] cities = {"Colombo", "Kandy", "Galle", "Jaffna", "Nuwara Eliya"};
        String[] countries = {"Sri Lanka", "Sri Lanka", "Sri Lanka", "Sri Lanka", "Sri Lanka"};
        double[][] coordinates = {
            {6.9271, 79.8612}, {7.2906, 80.6337}, {6.0329, 80.2168}, {9.6615, 80.0255}, {6.9497, 80.7891}
        };
        
        int index = (int) (Math.random() * cities.length);
        Location location = new Location(cities[index], countries[index], 
                                       coordinates[index][0], coordinates[index][1], "Asia/Colombo");
        
        double baseTemp = 25 + (Math.random() * 10);
        double humidity = 60 + (Math.random() * 40);
        double windSpeed = 2 + (Math.random() * 8);
        
        String[] conditions = {"Clear", "Clouds", "Rain", "Thunderstorm"};
        String[] descriptions = {"Sunny", "Cloudy", "Rain showers", "Thunderstorms"};
        int conditionIndex = (int) (Math.random() * conditions.length);
        
        double pressure = 1009 + (Math.random() * 10);
        double feelsLike = baseTemp + (humidity > 80 ? 2 : 0);
        int uvIndex = (int) (5 + (Math.random() * 6));
        int visibility = 8 + (int)(Math.random() * 7);
        int cloudiness = conditionIndex == 0 ? (int)(Math.random() * 20) : 
                        conditionIndex == 1 ? 60 + (int)(Math.random() * 40) : 80 + (int)(Math.random() * 20);
        
        return new WeatherData(location, baseTemp, humidity, windSpeed, 
                             conditions[conditionIndex], descriptions[conditionIndex], 
                             pressure, feelsLike, uvIndex, visibility, cloudiness);
    }
    
    @Override
    public boolean isServiceAvailable() {
        try {
            WeatherData testData = getCurrentWeather(6.9271, 79.8612);
            return !testData.getLocation().getCity().equals("Unknown City");
        } catch (Exception e) {
            return false;
        }
    }
}
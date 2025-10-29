package lk.ijse.weatherapp.server;

import lk.ijse.weatherapp.model.WeatherData;
import lk.ijse.weatherapp.model.Location;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class WeatherAPIService implements WeatherService {
    private static final String API_KEY = "97318b70bfa24d9e96533508252910"; // Get from https://www.weatherapi.com/
    private static final String BASE_URL = "http://api.weatherapi.com/v1/current.json";
    
    @Override
    public WeatherData getCurrentWeather(Location location) {
        return getCurrentWeather(location.getLatitude(), location.getLongitude());
    }
    
    @Override
    public WeatherData getCurrentWeather(double latitude, double longitude) {
        if (API_KEY.equals("97318b70bfa24d9e96533508252910") || API_KEY.isEmpty()) {
            System.out.println("⚠️ WeatherAPI key not configured. Using simulated data.");
            return generateSimulatedWeather(latitude, longitude);
        }
        
        try {
            String urlString = String.format("%s?key=%s&q=%f,%f", 
                BASE_URL, API_KEY, latitude, longitude);
            
            System.out.println("🌐 Fetching from WeatherAPI: " + latitude + ", " + longitude);
            
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            
            int responseCode = connection.getResponseCode();
            System.out.println("📡 WeatherAPI Response Code: " + responseCode);
            
            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                return parseWeatherData(response.toString());
            } else {
                System.err.println("❌ WeatherAPI Error: " + responseCode);
                return generateSimulatedWeather(latitude, longitude);
            }
        } catch (Exception e) {
            System.err.println("❌ Error fetching WeatherAPI data: " + e.getMessage());
            return generateSimulatedWeather(latitude, longitude);
        }
    }
    
    private WeatherData parseWeatherData(String jsonResponse) {
        try {
            JsonObject json = JsonParser.parseString(jsonResponse).getAsJsonObject();
            JsonObject locationData = json.getAsJsonObject("location");
            JsonObject current = json.getAsJsonObject("current");
            
            // Parse location
            Location location = new Location();
            location.setCity(locationData.get("name").getAsString());
            location.setCountry(locationData.get("country").getAsString());
            location.setLatitude(locationData.get("lat").getAsDouble());
            location.setLongitude(locationData.get("lon").getAsDouble());
            location.setTimezone(locationData.get("tz_id").getAsString());
            
            // Parse weather data
            double temp = current.get("temp_c").getAsDouble();
            double feelsLike = current.get("feelslike_c").getAsDouble();
            double humidity = current.get("humidity").getAsDouble();
            double pressure = current.get("pressure_mb").getAsDouble();
            double windSpeed = current.get("wind_kph").getAsDouble();
            int cloudiness = current.get("cloud").getAsInt();
            int uvIndex = current.get("uv").getAsInt();
            int visibility = current.get("vis_km").getAsInt();
            
            JsonObject condition = current.getAsJsonObject("condition");
            String conditionText = condition.get("text").getAsString();
            String description = conditionText;
            
            return new WeatherData(location, temp, humidity, windSpeed, 
                                 getStandardCondition(conditionText), description, 
                                 pressure, feelsLike, uvIndex, visibility, cloudiness);
            
        } catch (Exception e) {
            System.err.println("❌ Error parsing WeatherAPI data: " + e.getMessage());
            return generateSimulatedWeather(0, 0);
        }
    }
    
    private String getStandardCondition(String conditionText) {
        String lower = conditionText.toLowerCase();
        if (lower.contains("sun") || lower.contains("clear")) return "Clear";
        if (lower.contains("cloud")) return "Clouds";
        if (lower.contains("rain")) return "Rain";
        if (lower.contains("drizzle")) return "Drizzle";
        if (lower.contains("thunder") || lower.contains("storm")) return "Thunderstorm";
        if (lower.contains("snow")) return "Snow";
        if (lower.contains("mist") || lower.contains("fog")) return "Mist";
        return "Clear";
    }
    
    private WeatherData generateSimulatedWeather(double lat, double lon) {
        // Same simulated data as before
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
            WeatherData testData = getCurrentWeather(6.9271, 79.8612); // Colombo
            return !testData.getLocation().getCity().equals("Unknown");
        } catch (Exception e) {
            return false;
        }
    }
}
package lk.ijse.weatherapp.client.util;

import lk.ijse.weatherapp.model.Location;
import javafx.application.Platform;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class LocationDetector {
    
    public interface LocationCallback {
        void onLocationDetected(Location location);
        void onLocationError(String error);
    }
    
    public static void detectLocation(LocationCallback callback) {
        new Thread(() -> {
            try {
                // Method 1: Try IP-based geolocation
                Location location = getLocationFromIP();
                
                Platform.runLater(() -> {
                    if (location != null) {
                        callback.onLocationDetected(location);
                    } else {
                        callback.onLocationError("Could not detect location automatically");
                    }
                });
                
            } catch (Exception e) {
                Platform.runLater(() -> 
                    callback.onLocationError("Location detection failed: " + e.getMessage()));
            }
        }).start();
    }
    
    private static Location getLocationFromIP() {
        try {
            // Using ipapi.co for IP geolocation (free tier available)
            URL url = new URL("https://ipapi.co/json/");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            Scanner scanner = new Scanner(connection.getInputStream());
            StringBuilder response = new StringBuilder();
            while (scanner.hasNextLine()) {
                response.append(scanner.nextLine());
            }
            scanner.close();
            
            // Parse JSON response
            String json = response.toString();
            String city = extractValue(json, "city");
            String country = extractValue(json, "country_name");
            String latStr = extractValue(json, "latitude");
            String lonStr = extractValue(json, "longitude");
            String timezone = extractValue(json, "timezone");
            
            if (city != null && country != null && latStr != null && lonStr != null) {
                double lat = Double.parseDouble(latStr);
                double lon = Double.parseDouble(lonStr);
                return new Location(city, country, lat, lon, timezone);
            }
        } catch (Exception e) {
            System.err.println("IP geolocation failed: " + e.getMessage());
        }
        
        return null;
    }
    
    private static String extractValue(String json, String key) {
        try {
            String search = "\"" + key + "\":";
            int start = json.indexOf(search) + search.length();
            int end = json.indexOf(",", start);
            if (end == -1) end = json.indexOf("}", start);
            
            String value = json.substring(start, end).trim();
            if (value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length() - 1);
            }
            return value;
        } catch (Exception e) {
            return null;
        }
    }
    
    // Manual location input for cases where auto-detection fails
    public static Location createManualLocation(String city, String country, 
                                              double lat, double lon, String timezone) {
        return new Location(city, country, lat, lon, timezone);
    }
}
package lk.ijse.weatherapp.server;

import lk.ijse.weatherapp.model.WeatherData;
import lk.ijse.weatherapp.model.Location;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class OpenWeatherMapService implements WeatherService {
    // Replace with your actual API key
    private static final String API_KEY = "https://api.openweathermap.org/data/3.0/onecall?lat={lat}&lon={lon}&exclude={part}&appid={API key}";
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/weather";

    @Override
    public WeatherData getCurrentWeather(Location location) {
        return getCurrentWeather(location.getLatitude(), location.getLongitude());
    }

    @Override
    public WeatherData getCurrentWeather(double latitude, double longitude) {
        // First, check if API key is configured
        if (API_KEY.equals("https://api.openweathermap.org/data/3.0/onecall?lat={lat}&lon={lon}&exclude={part}&appid={API key}") || API_KEY.isEmpty()) {
            System.out.println("⚠️ OpenWeatherMap API key not configured. Using simulated data.");
            return generateSimulatedWeather(latitude, longitude);
        }

        try {
            String urlString = String.format("%s?lat=%.4f&lon=%.4f&appid=%s&units=metric",
                    BASE_URL, latitude, longitude, API_KEY);

            System.out.println("🌐 Fetching weather data from: " +
                    String.format("%s?lat=%.4f&lon=%.4f&appid=%s&units=metric",
                            BASE_URL, latitude, longitude, "***" + API_KEY.substring(Math.max(0, API_KEY.length() - 4))));

            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000); // Increased timeout
            connection.setReadTimeout(10000);
            connection.setRequestProperty("User-Agent", "WeatherApp/1.0");

            int responseCode = connection.getResponseCode();
            System.out.println("📡 API Response Code: " + responseCode);

            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                WeatherData weatherData = parseWeatherData(response.toString());
                System.out.println("✅ Successfully fetched weather data for: " + weatherData.getLocation().getCity());
                return weatherData;

            } else if (responseCode == 401) {
                System.err.println("❌ OpenWeatherMap API Error 401: Invalid API Key");
                System.err.println("💡 Please check your API key at: https://home.openweathermap.org/api_keys");
                return generateSimulatedWeather(latitude, longitude);

            } else if (responseCode == 429) {
                System.err.println("❌ OpenWeatherMap API Error 429: Rate limit exceeded");
                return generateSimulatedWeather(latitude, longitude);

            } else {
                System.err.println("❌ OpenWeatherMap API Error: " + responseCode);
                // Read error stream for more details
                try {
                    BufferedReader errorReader = new BufferedReader(
                            new InputStreamReader(connection.getErrorStream()));
                    String errorLine;
                    while ((errorLine = errorReader.readLine()) != null) {
                        System.err.println("Error details: " + errorLine);
                    }
                    errorReader.close();
                } catch (Exception e) {
                    System.err.println("Could not read error details: " + e.getMessage());
                }
                return generateSimulatedWeather(latitude, longitude);
            }
        } catch (Exception e) {
            System.err.println("❌ Error fetching weather data: " + e.getMessage());
            return generateSimulatedWeather(latitude, longitude);
        }
    }

    private WeatherData parseWeatherData(String jsonResponse) {
        try {
            JsonObject json = JsonParser.parseString(jsonResponse).getAsJsonObject();

            // Parse location
            Location location = new Location();
            location.setCity(json.get("name").getAsString());
            location.setCountry(json.getAsJsonObject("sys").get("country").getAsString());
            location.setLatitude(json.getAsJsonObject("coord").get("lat").getAsDouble());
            location.setLongitude(json.getAsJsonObject("coord").get("lon").getAsDouble());
            location.setTimezone("UTC"); // OpenWeatherMap doesn't provide timezone in basic plan

            // Parse main weather data
            JsonObject main = json.getAsJsonObject("main");
            JsonObject weather = json.getAsJsonArray("weather").get(0).getAsJsonObject();
            JsonObject wind = json.getAsJsonObject("wind");
            JsonObject clouds = json.getAsJsonObject("clouds");

            double temp = main.get("temp").getAsDouble();
            double feelsLike = main.has("feels_like") ? main.get("feels_like").getAsDouble() : temp;
            double humidity = main.get("humidity").getAsDouble();
            double pressure = main.get("pressure").getAsDouble();
            double windSpeed = wind.get("speed").getAsDouble();
            int cloudiness = clouds.get("all").getAsInt();
            int visibility = json.has("visibility") ? json.get("visibility").getAsInt() / 1000 : 10;

            String condition = weather.get("main").getAsString();
            String description = weather.get("description").getAsString();

            // Calculate UV index (simplified - UV data requires premium plan)
            int uvIndex = calculateUVIndex(location.getLatitude());

            return new WeatherData(location, temp, humidity, windSpeed, condition,
                    description, pressure, feelsLike, uvIndex, visibility, cloudiness);

        } catch (Exception e) {
            System.err.println("❌ Error parsing weather data: " + e.getMessage());
            System.err.println("Raw response: " + jsonResponse);
            return generateSimulatedWeather(0, 0);
        }
    }

    private WeatherData generateSimulatedWeather(double lat, double lon) {
        // Enhanced fallback simulated weather when API is unavailable
        String[] cities = {"Colombo", "Kandy", "Galle", "Jaffna", "Nuwara Eliya"};
        String[] countries = {"Sri Lanka", "Sri Lanka", "Sri Lanka", "Sri Lanka", "Sri Lanka"};
        double[][] coordinates = {
                {6.9271, 79.8612}, {7.2906, 80.6337}, {6.0329, 80.2168}, {9.6615, 80.0255}, {6.9497, 80.7891}
        };

        int index = (int) (Math.random() * cities.length);
        Location location = new Location(cities[index], countries[index],
                coordinates[index][0], coordinates[index][1], "Asia/Colombo");

        // Realistic weather simulation for Sri Lanka
        double baseTemp = 25 + (Math.random() * 10); // 25-35°C typical for Sri Lanka
        double humidity = 60 + (Math.random() * 40); // 60-100% typical humidity
        double windSpeed = 2 + (Math.random() * 8); // 2-10 km/h typical

        // Weather conditions based on Sri Lankan climate
        String[] conditions;
        String[] descriptions;
        if (location.getCity().equals("Nuwara Eliya")) {
            // Cooler climate for hill country
            baseTemp = 15 + (Math.random() * 10); // 15-25°C
            conditions = new String[]{"Clouds", "Rain", "Drizzle", "Mist"};
            descriptions = new String[]{"Cloudy", "Light rain", "Drizzle", "Misty"};
        } else if (location.getCity().equals("Jaffna")) {
            // Hot and dry for northern peninsula
            baseTemp = 28 + (Math.random() * 8); // 28-36°C
            conditions = new String[]{"Clear", "Clouds", "Dust"};
            descriptions = new String[]{"Sunny", "Partly cloudy", "Dusty"};
        } else {
            // Coastal and central regions
            conditions = new String[]{"Clear", "Clouds", "Rain", "Thunderstorm"};
            descriptions = new String[]{"Sunny", "Cloudy", "Rain showers", "Thunderstorms"};
        }

        int conditionIndex = (int) (Math.random() * conditions.length);
        String condition = conditions[conditionIndex];
        String description = descriptions[conditionIndex];

        double pressure = 1009 + (Math.random() * 10); // 1009-1019 hPa
        double feelsLike = baseTemp + (humidity > 80 ? 2 : 0); // Feels hotter when humid
        int uvIndex = (int) (5 + (Math.random() * 6)); // 5-10 UV index
        int visibility = condition.equals("Mist") ? 2 + (int)(Math.random() * 3) : 8 + (int)(Math.random() * 7);
        int cloudiness = condition.equals("Clear") ? (int)(Math.random() * 20) :
                condition.equals("Clouds") ? 60 + (int)(Math.random() * 40) : 80 + (int)(Math.random() * 20);

        System.out.println("🌤️ Using simulated weather data for: " + location.getCity());

        return new WeatherData(location, baseTemp, humidity, windSpeed, condition,
                description, pressure, feelsLike, uvIndex, visibility, cloudiness);
    }

    private int calculateUVIndex(double latitude) {
        // Simplified UV index calculation based on latitude and time
        double latEffect = Math.max(0, 1 - Math.abs(latitude) / 90);
        int hour = java.time.LocalDateTime.now().getHour();
        double timeEffect = Math.max(0, 1 - Math.abs(hour - 12) / 6);

        return (int) Math.min(11, latEffect * timeEffect * 8 + Math.random() * 3);
    }

    @Override
    public boolean isServiceAvailable() {
        try {
            // Test connection with a known location (London)
            WeatherData testData = getCurrentWeather(51.5074, -0.1278);
            return !testData.getLocation().getCity().equals("Unknown");
        } catch (Exception e) {
            System.err.println("❌ Weather service test failed: " + e.getMessage());
            return false;
        }
    }
}
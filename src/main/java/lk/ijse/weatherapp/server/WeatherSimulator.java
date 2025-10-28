package lk.ijse.weatherapp.server;

import lk.ijse.weatherapp.model.WeatherData;
import lk.ijse.weatherapp.model.Location;
import java.util.Random;

public class WeatherSimulator {
    private final Random random;
    private Location currentLocation;

    // Climate zones and their characteristics
    private enum ClimateZone {
        TROPICAL(25, 35, 70, 95, 5, 25, 1005, 1015),
        TEMPERATE(5, 25, 50, 85, 10, 35, 1010, 1020),
        CONTINENTAL(-10, 30, 30, 80, 15, 50, 1000, 1020),
        ARID(15, 45, 10, 40, 5, 30, 1005, 1015),
        POLAR(-30, 10, 50, 90, 20, 60, 980, 1010);

        final double minTemp, maxTemp;
        final double minHumidity, maxHumidity;
        final double minWind, maxWind;
        final double minPressure, maxPressure;

        ClimateZone(double minTemp, double maxTemp, double minHumidity, double maxHumidity,
                    double minWind, double maxWind, double minPressure, double maxPressure) {
            this.minTemp = minTemp;
            this.maxTemp = maxTemp;
            this.minHumidity = minHumidity;
            this.maxHumidity = maxHumidity;
            this.minWind = minWind;
            this.maxWind = maxWind;
            this.minPressure = minPressure;
            this.maxPressure = maxPressure;
        }
    }

    public WeatherSimulator() {
        this.random = new Random();
        this.currentLocation = new Location("Colombo", "Sri Lanka", 6.9271, 79.8612, "Asia/Colombo");
    }

    public WeatherData generateWeatherData() {
        // Change location occasionally (5% chance)
        if (random.nextDouble() < 0.05) {
            currentLocation = getRandomLocation();
        }

        ClimateZone zone = getClimateZone(currentLocation);

        // Generate weather based on climate zone and season
        double temperature = generateTemperature(zone);
        double humidity = generateHumidity(zone, temperature);
        double windSpeed = generateWindSpeed(zone);
        double pressure = generatePressure(zone);
        String condition = generateWeatherCondition(zone, humidity, temperature);
        String description = generateWeatherDescription(condition);
        double feelsLike = calculateFeelsLike(temperature, humidity, windSpeed);
        int uvIndex = generateUVIndex(currentLocation.getLatitude());
        int visibility = generateVisibility(condition, humidity);
        int cloudiness = generateCloudiness(condition);

        return new WeatherData(
                currentLocation,
                Math.round(temperature * 10) / 10.0,
                Math.round(humidity * 10) / 10.0,
                Math.round(windSpeed * 10) / 10.0,
                condition,
                description,
                Math.round(pressure * 10) / 10.0,
                Math.round(feelsLike * 10) / 10.0,
                uvIndex,
                visibility,
                cloudiness
        );
    }

    private Location getRandomLocation() {
        String[][] locations = {
                {"Colombo", "Sri Lanka", "6.9271", "79.8612", "Asia/Colombo"},
                {"Kandy", "Sri Lanka", "7.2906", "80.6337", "Asia/Colombo"},
                {"Galle", "Sri Lanka", "6.0329", "80.2168", "Asia/Colombo"},
                {"Jaffna", "Sri Lanka", "9.6615", "80.0255", "Asia/Colombo"},
                {"Nuwara Eliya", "Sri Lanka", "6.9497", "80.7891", "Asia/Colombo"}
        };

        int index = random.nextInt(locations.length);
        String[] loc = locations[index];
        return new Location(loc[0], loc[1], Double.parseDouble(loc[2]), Double.parseDouble(loc[3]), loc[4]);
    }

    private ClimateZone getClimateZone(Location location) {
        double lat = Math.abs(location.getLatitude());

        if (lat < 23.5) return ClimateZone.TROPICAL;
        else if (lat < 35) return location.getCountry().equals("Sri Lanka") ? ClimateZone.TROPICAL : ClimateZone.ARID;
        else if (lat < 50) return ClimateZone.TEMPERATE;
        else if (lat < 60) return ClimateZone.CONTINENTAL;
        else return ClimateZone.POLAR;
    }

    private double generateTemperature(ClimateZone zone) {
        double baseTemp = zone.minTemp + (zone.maxTemp - zone.minTemp) * random.nextDouble();

        // Add daily variation (colder at night, warmer during day)
        int hour = java.time.LocalDateTime.now().getHour();
        double dailyVariation = Math.sin((hour - 6) * Math.PI / 12) * 8; // ±8°C variation

        // Add seasonal variation based on hemisphere
        boolean southernHemisphere = currentLocation.getLatitude() < 0;
        int month = java.time.LocalDateTime.now().getMonthValue();
        double seasonalVariation = Math.sin((month - (southernHemisphere ? 6 : 0)) * Math.PI / 6) * 10; // ±10°C seasonal

        return baseTemp + dailyVariation + seasonalVariation;
    }

    private double generateHumidity(ClimateZone zone, double temperature) {
        double baseHumidity = zone.minHumidity + (zone.maxHumidity - zone.minHumidity) * random.nextDouble();

        // Humidity decreases as temperature increases
        double tempEffect = (35 - temperature) * 0.5;

        // Higher humidity at night
        int hour = java.time.LocalDateTime.now().getHour();
        double dailyVariation = Math.cos((hour - 6) * Math.PI / 12) * 10;

        double humidity = baseHumidity + tempEffect + dailyVariation;
        return Math.max(zone.minHumidity, Math.min(zone.maxHumidity, humidity));
    }

    private double generateWindSpeed(ClimateZone zone) {
        double baseWind = zone.minWind + (zone.maxWind - zone.minWind) * random.nextDouble();

        // Higher wind speeds during day due to thermal effects
        int hour = java.time.LocalDateTime.now().getHour();
        double dailyVariation = Math.sin((hour - 12) * Math.PI / 12) * 5;

        // Coastal areas have higher wind speeds
        boolean isCoastal = isCoastalLocation(currentLocation);
        double coastalEffect = isCoastal ? 5 : 0;

        return Math.max(0, baseWind + dailyVariation + coastalEffect);
    }

    private double generatePressure(ClimateZone zone) {
        double basePressure = zone.minPressure + (zone.maxPressure - zone.minPressure) * random.nextDouble();

        // Pressure decreases with altitude
        double altitudeEffect = -getAltitude(currentLocation) * 0.1;

        return basePressure + altitudeEffect + (random.nextDouble() - 0.5) * 5;
    }

    private String generateWeatherCondition(ClimateZone zone, double humidity, double temperature) {
        double chance = random.nextDouble();

        if (zone == ClimateZone.TROPICAL) {
            if (humidity > 85 && chance < 0.3) return "Thunderstorm";
            else if (humidity > 80 && chance < 0.5) return "Rain";
            else if (humidity > 75 && chance < 0.7) return "Drizzle";
            else if (humidity > 70) return "Clouds";
            else return "Clear";
        }
        else if (zone == ClimateZone.ARID) {
            if (chance < 0.1) return "Clouds";
            else return "Clear";
        }
        else if (zone == ClimateZone.POLAR) {
            if (temperature < -5 && chance < 0.4) return "Snow";
            else if (temperature < 0 && chance < 0.6) return "Rain";
            else return "Clouds";
        }
        else {
            if (humidity > 85 && chance < 0.2) return "Thunderstorm";
            else if (humidity > 80 && chance < 0.4) return "Rain";
            else if (humidity > 70 && chance < 0.6) return "Drizzle";
            else if (humidity > 60 && chance < 0.8) return "Clouds";
            else if (chance < 0.1) return "Mist";
            else return "Clear";
        }
    }

    private String generateWeatherDescription(String condition) {
        switch (condition) {
            case "Clear": return "Clear sky";
            case "Clouds": return "Cloudy";
            case "Rain": return "Moderate rain";
            case "Drizzle": return "Light drizzle";
            case "Thunderstorm": return "Thunderstorm with rain";
            case "Snow": return "Light snow";
            case "Mist": return "Misty conditions";
            default: return "Fair weather";
        }
    }

    private double calculateFeelsLike(double temperature, double humidity, double windSpeed) {
        // Heat index calculation for high temperatures
        if (temperature > 27) {
            // Simplified heat index formula
            double heatIndex = temperature + 0.5 * (humidity / 100) * (temperature - 20);
            return Math.max(temperature, heatIndex);
        }
        // Wind chill for low temperatures
        else if (temperature < 10 && windSpeed > 5) {
            // Simplified wind chill formula
            double windChill = 13.12 + 0.6215 * temperature - 11.37 * Math.pow(windSpeed, 0.16)
                    + 0.3965 * temperature * Math.pow(windSpeed, 0.16);
            return Math.min(temperature, windChill);
        }
        return temperature;
    }

    private int generateUVIndex(double latitude) {
        // UV index is higher near equator and during midday
        double latEffect = Math.max(0, 1 - Math.abs(latitude) / 90);
        int hour = java.time.LocalDateTime.now().getHour();
        double timeEffect = Math.max(0, 1 - Math.abs(hour - 12) / 6);

        return (int) Math.round(latEffect * timeEffect * 11); // 0-11 scale
    }

    private int generateVisibility(String condition, double humidity) {
        switch (condition) {
            case "Mist": case "Fog": return 2 + random.nextInt(3); // 2-5 km
            case "Rain": case "Drizzle": return 5 + random.nextInt(5); // 5-10 km
            case "Thunderstorm": return 3 + random.nextInt(4); // 3-7 km
            default: return 8 + random.nextInt(7); // 8-15 km
        }
    }

    private int generateCloudiness(String condition) {
        switch (condition) {
            case "Clear": return random.nextInt(20); // 0-20%
            case "Clouds": return 60 + random.nextInt(40); // 60-100%
            case "Rain": case "Drizzle": case "Thunderstorm": return 80 + random.nextInt(20); // 80-100%
            case "Mist": case "Fog": return 40 + random.nextInt(40); // 40-80%
            default: return random.nextInt(100);
        }
    }

    private boolean isCoastalLocation(Location location) {
        // Simplified coastal detection
        String[] coastalCities = {"colombo", "galle"};
        for (String city : coastalCities) {
            if (location.getCity().equalsIgnoreCase(city)) return true;
        }
        return false;
    }

    private double getAltitude(Location location) {
        // Simplified altitude data
        switch (location.getCity().toLowerCase()) {
            case "nuwara eliya": return 1868; // meters
            case "kandy": return 500;
            case "colombo": return 1;
            case "galle": return 15;
            case "jaffna": return 10;
            default: return 100;
        }
    }

    public Location getCurrentLocation() {
        return currentLocation;
    }
}
package lk.ijse.weatherapp.server;

import lk.ijse.weatherapp.model.WeatherData;
import lk.ijse.weatherapp.model.Location;
import java.io.*;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;
    private final AtomicBoolean isRunning;
    private final int clientId;
    private final WeatherService weatherService;
    private Location clientLocation;

    public ClientHandler(Socket socket, AtomicBoolean isRunning, int clientId, WeatherService weatherService) {
        this.clientSocket = socket;
        this.isRunning = isRunning;
        this.clientId = clientId;
        this.weatherService = weatherService; // Make sure this line exists

        try {
            this.inputStream = new ObjectInputStream(clientSocket.getInputStream());
            this.outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
            System.out.println("Client #" + clientId + " connected from: " +
                    clientSocket.getInetAddress().getHostAddress());
        } catch (IOException e) {
            System.err.println("Error creating streams for client #" + clientId + ": " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            // Receive client's location
            receiveClientLocation();

            // Send initial weather data
            sendCurrentWeather();

            // Keep connection alive and handle weather updates
            while (isRunning.get() && !clientSocket.isClosed()) {
                Thread.sleep(30000); // Update every 30 seconds
                sendCurrentWeather();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Client handler #" + clientId + " interrupted");
        } catch (IOException e) {
            System.err.println("Client #" + clientId + " connection error: " + e.getMessage());
        } finally {
            close();
        }
    }

    private void receiveClientLocation() throws IOException {
        try {
            Object obj = inputStream.readObject();
            if (obj instanceof Location) {
                this.clientLocation = (Location) obj;
                System.out.println("Client #" + clientId + " location: " + clientLocation);
            }
        } catch (ClassNotFoundException e) {
            System.err.println("Invalid location data from client #" + clientId);
            // Use IP-based location as fallback
            this.clientLocation = getLocationFromIP();
        }
    }

    private Location getLocationFromIP() {
        // Simple IP-based location (in real app, use IP geolocation service)
        String ip = clientSocket.getInetAddress().getHostAddress();

        // Default to Colombo, Sri Lanka for local connections
        if (ip.equals("127.0.0.1")) {
            return new Location("Colombo", "Sri Lanka", 6.9271, 79.8612, "Asia/Colombo");
        }

        // For other IPs, use a default location
        return new Location("Unknown", "Unknown", 0, 0, "UTC");
    }

    private void sendCurrentWeather() {
        if (clientLocation == null) return;

        WeatherData weatherData = weatherService.getCurrentWeather(clientLocation);

        try {
            outputStream.writeObject(weatherData);
            outputStream.flush();
            outputStream.reset();

            System.out.println("Sent weather to Client #" + clientId + ": " +
                    weatherData.getLocation().getCity() + " - " + weatherData.getTemperature() + "°C");
        } catch (IOException e) {
            System.err.println("Error sending weather to client #" + clientId + ": " + e.getMessage());
        }
    }

    public void close() {
        try {
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
            if (clientSocket != null) clientSocket.close();
            System.out.println("Client #" + clientId + " disconnected");
        } catch (IOException e) {
            System.err.println("Error closing client connection #" + clientId + ": " + e.getMessage());
        }
    }

    public int getClientId() {
        return clientId;
    }

    public boolean isConnected() {
        return clientSocket != null && !clientSocket.isClosed();
    }
}
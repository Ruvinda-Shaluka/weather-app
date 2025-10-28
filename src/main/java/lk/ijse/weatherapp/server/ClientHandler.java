package lk.ijse.weatherapp.server;

import lk.ijse.weatherapp.model.WeatherData;
import java.io.*;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private ObjectOutputStream outputStream;
    private final AtomicBoolean isRunning;
    private final int clientId;
    
    public ClientHandler(Socket socket, AtomicBoolean isRunning, int clientId) {
        this.clientSocket = socket;
        this.isRunning = isRunning;
        this.clientId = clientId;
        
        try {
            this.outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
            System.out.println("Client #" + clientId + " connected from: " + 
                clientSocket.getInetAddress().getHostAddress());
        } catch (IOException e) {
            System.err.println("Error creating output stream for client #" + clientId + ": " + e.getMessage());
        }
    }
    
    @Override
    public void run() {
        try {
            // Send welcome message
            sendWelcomeMessage();
            
            // Keep connection alive and handle client
            while (isRunning.get() && !clientSocket.isClosed()) {
                Thread.sleep(1000); // Check every second if still connected
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Client handler #" + clientId + " interrupted");
        } finally {
            close();
        }
    }
    
    private void sendWelcomeMessage() {
        try {
            WeatherData welcomeData = new WeatherData(0, 0, 0, "Connected to Weather Server");
            outputStream.writeObject(welcomeData);
            outputStream.flush();
        } catch (IOException e) {
            System.err.println("Error sending welcome message to client #" + clientId + ": " + e.getMessage());
        }
    }
    
    public boolean sendWeatherData(WeatherData weatherData) {
        if (outputStream == null) return false;
        
        try {
            outputStream.writeObject(weatherData);
            outputStream.flush();
            outputStream.reset(); // Clear cache for new object
            return true;
        } catch (IOException e) {
            System.err.println("Error sending data to client #" + clientId + ": " + e.getMessage());
            return false;
        }
    }
    
    public void close() {
        try {
            if (outputStream != null) {
                outputStream.close();
            }
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
            System.out.println("Client #" + clientId + " disconnected");
        } catch (IOException e) {
            System.err.println("Error closing client connection #" + clientId + ": " + e.getMessage());
        }
    }
    
    public int getClientId() {
        return clientId;
    }
    
    public boolean isConnected() {
        return clientSocket != null && !clientSocket.isClosed() && outputStream != null;
    }
}
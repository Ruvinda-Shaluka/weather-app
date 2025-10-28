package lk.ijse.weatherapp.server;

import lk.ijse.weatherapp.model.WeatherData;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class WeatherServer {
    private static final int DEFAULT_PORT = 8080;
    private ServerSocket serverSocket;
    private final List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final AtomicBoolean isRunning = new AtomicBoolean(true);
    private final WeatherSimulator weatherSimulator;
    private int clientCounter = 0;
    
    public WeatherServer() {
        this.weatherSimulator = new WeatherSimulator();
    }
    
    public void start(int port) {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("🌤️ Weather Server started on port " + port);
            System.out.println("📍 Server Address: " + InetAddress.getLocalHost().getHostAddress() + ":" + port);
            
            // Schedule weather data broadcasting every 10 seconds
            scheduler.scheduleAtFixedRate(this::broadcastWeatherData, 0, 10, TimeUnit.SECONDS);
            
            // Schedule client cleanup every 30 seconds
            scheduler.scheduleAtFixedRate(this::cleanupDisconnectedClients, 30, 30, TimeUnit.SECONDS);
            
            // Accept client connections
            while (isRunning.get()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    ClientHandler clientHandler = new ClientHandler(clientSocket, isRunning, ++clientCounter);
                    clients.add(clientHandler);
                    new Thread(clientHandler).start();
                    
                    System.out.println("✅ Client #" + clientCounter + " connected. Total clients: " + clients.size());
                } catch (SocketException e) {
                    if (isRunning.get()) {
                        System.err.println("Socket error while accepting client: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("❌ Server error: " + e.getMessage());
        } finally {
            stop();
        }
    }
    
    private void broadcastWeatherData() {
        if (clients.isEmpty()) {
            System.out.println("⏳ No clients connected. Waiting...");
            return;
        }
        
        // Generate simulated weather data
        WeatherData weatherData = weatherSimulator.generateWeatherData();
        System.out.println("📡 Broadcasting: " + weatherData);
        
        // Broadcast to all connected clients
        List<ClientHandler> clientsToRemove = new ArrayList<>();
        
        synchronized (clients) {
            for (ClientHandler client : clients) {
                if (!client.sendWeatherData(weatherData)) {
                    clientsToRemove.add(client);
                    System.out.println("❌ Client #" + client.getClientId() + " failed to receive data. Marked for removal.");
                }
            }
            
            // Remove disconnected clients
            clients.removeAll(clientsToRemove);
        }
        
        // Close connections for removed clients
        for (ClientHandler client : clientsToRemove) {
            client.close();
        }
        
        if (!clientsToRemove.isEmpty()) {
            System.out.println("🔄 Removed " + clientsToRemove.size() + " disconnected clients. Total clients: " + clients.size());
        }
    }
    
    private void cleanupDisconnectedClients() {
        synchronized (clients) {
            Iterator<ClientHandler> iterator = clients.iterator();
            int removedCount = 0;
            
            while (iterator.hasNext()) {
                ClientHandler client = iterator.next();
                if (!client.isConnected()) {
                    iterator.remove();
                    client.close();
                    removedCount++;
                }
            }
            
            if (removedCount > 0) {
                System.out.println("🧹 Cleanup: Removed " + removedCount + " disconnected clients. Total clients: " + clients.size());
            }
        }
    }
    
    public void stop() {
        System.out.println("🛑 Shutting down Weather Server...");
        isRunning.set(false);
        
        // Shutdown scheduler
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // Close all client connections
        synchronized (clients) {
            for (ClientHandler client : clients) {
                client.close();
            }
            clients.clear();
        }
        
        // Close server socket
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
        }
        
        System.out.println("✅ Weather Server stopped successfully.");
    }
    
    public static void main(String[] args) {
        WeatherServer server = new WeatherServer();
        
        // Add shutdown hook for graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number. Using default: " + DEFAULT_PORT);
            }
        }
        
        try {
            server.start(port);
        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
        }
    }
}
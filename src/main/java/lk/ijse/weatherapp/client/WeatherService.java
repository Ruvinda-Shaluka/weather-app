package lk.ijse.weatherapp.client;

import lk.ijse.weatherapp.model.WeatherData;
import java.io.*;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class WeatherService {
    private Socket socket;
    private ObjectInputStream inputStream;
    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    private final BlockingQueue<WeatherData> weatherDataQueue = new LinkedBlockingQueue<>();
    private String serverIp;
    private int serverPort;
    private Thread listenerThread;
    
    public interface WeatherDataListener {
        void onWeatherDataReceived(WeatherData data);
        void onConnectionStatusChanged(boolean connected, String message);
    }
    
    private WeatherDataListener listener;
    
    public void setWeatherDataListener(WeatherDataListener listener) {
        this.listener = listener;
    }
    
    public boolean connect(String ip, int port) {
        this.serverIp = ip;
        this.serverPort = port;
        
        try {
            socket = new Socket(ip, port);
            inputStream = new ObjectInputStream(socket.getInputStream());
            isConnected.set(true);
            
            // Start listening for weather data
            startListenerThread();
            
            if (listener != null) {
                listener.onConnectionStatusChanged(true, "Connected to " + ip + ":" + port);
            }
            
            return true;
        } catch (IOException e) {
            if (listener != null) {
                listener.onConnectionStatusChanged(false, "Connection failed: " + e.getMessage());
            }
            return false;
        }
    }
    
    private void startListenerThread() {
        listenerThread = new Thread(() -> {
            while (isConnected.get() && !socket.isClosed()) {
                try {
                    WeatherData weatherData = (WeatherData) inputStream.readObject();
                    weatherDataQueue.put(weatherData);
                    
                    if (listener != null) {
                        listener.onWeatherDataReceived(weatherData);
                    }
                } catch (EOFException e) {
                    break; // Server closed connection
                } catch (ClassNotFoundException e) {
                    System.err.println("Invalid data received: " + e.getMessage());
                } catch (IOException e) {
                    if (isConnected.get()) { // Only log if we didn't intentionally disconnect
                        System.err.println("Error reading data: " + e.getMessage());
                    }
                    break;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            disconnect();
        });
        
        listenerThread.setDaemon(true);
        listenerThread.start();
    }
    
    public void disconnect() {
        isConnected.set(false);
        
        try {
            if (inputStream != null) inputStream.close();
            if (socket != null) socket.close();
            if (listenerThread != null) listenerThread.interrupt();
        } catch (IOException e) {
            System.err.println("Error disconnecting: " + e.getMessage());
        }
        
        if (listener != null) {
            listener.onConnectionStatusChanged(false, "Disconnected");
        }
    }
    
    public boolean isConnected() {
        return isConnected.get() && socket != null && !socket.isClosed();
    }
    
    public WeatherData getLatestWeatherData() throws InterruptedException {
        return weatherDataQueue.take();
    }
    
    public boolean hasWeatherData() {
        return !weatherDataQueue.isEmpty();
    }
}
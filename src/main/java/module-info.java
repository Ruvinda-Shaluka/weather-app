module lk.ijse.weatherapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;

    // Export all necessary packages to JavaFX
    exports lk.ijse.weatherapp.client to javafx.graphics, javafx.fxml;
    exports lk.ijse.weatherapp.controller to javafx.fxml;
    exports lk.ijse.weatherapp.model to javafx.base;
    exports lk.ijse.weatherapp.server;

    // Open the package that contains FXML files
    opens lk.ijse.weatherapp.controller to javafx.fxml;
    opens lk.ijse.weatherapp.client to javafx.fxml;
}
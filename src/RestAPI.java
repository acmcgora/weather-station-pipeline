package api;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class RestAPI {

    // Update these to match your local database
    private static final String DB_URL = "jdbc:mysql://localhost:3306/weather";
    private static final String DB_USER = "user";
    private static final String DB_PASSWORD = "password";

    public void send(double temperature) {
        double rounded = Math.round(temperature * 10.0) / 10.0;
        System.out.println("REST API received temperature: " + rounded + " °C");

        // Store into MySQL
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {

            String sql = "INSERT INTO temperature (temperature, timestamp) VALUES (?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);

            stmt.setDouble(1, rounded);

            // Use current timestamp
            LocalDateTime now = LocalDateTime.now();
            stmt.setString(2, now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            stmt.executeUpdate();
            System.out.println("Temperature stored in database!");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

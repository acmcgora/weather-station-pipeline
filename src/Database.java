package database;

import java.sql.*;
import java.time.LocalDateTime;

public class Database {

    private static final String URL = "jdbc:mysql://localhost:3306/weather";
    private static final String USER = "user";
    private static final String PASSWORD = "password";

    public void save(double temperature) {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO temperature (temperature, timestamp) VALUES (?, ?)")) {

            stmt.setDouble(1, temperature);
            stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            stmt.executeUpdate();
            System.out.println("Database has stored value: " + String.format("%.1f °C", temperature));
        } catch (SQLException e) {
            System.out.println("DB save failed: " + e.getMessage());
        }
    }
}

package database;

import java.sql.*;

public class Database {

    // MySQL connection info
    private static final String URL = "jdbc:mysql://localhost:3306/weather";
    private static final String USER = "user";       // your MySQL username
    private static final String PASSWORD = "password"; // your MySQL password

    private Connection conn;

    public Database() {
        try {
            // Load the MySQL JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            // Connect to the database
            conn = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Connected to Weather database.");

            // Create table if it doesn't exist
            String createTable = """
                    CREATE TABLE IF NOT EXISTS temperature (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        temperature FLOAT NOT NULL,
                        timestamp DATETIME NOT NULL
                    );
                    """;
            Statement stmt = conn.createStatement();
            stmt.execute(createTable);
            stmt.close();

        } catch (ClassNotFoundException | SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
        }
    }

    //  Save a temperature reading
    public void save(double temperature) {
        String insertSQL = "INSERT INTO temperature (temperature, timestamp) VALUES (?, NOW())";
        try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            pstmt.setDouble(1, temperature);
            pstmt.executeUpdate();
            System.out.println("Database stored value: " + String.format("%.1f °C", temperature));
        } catch (SQLException e) {
            System.err.println("Failed to save temperature: " + e.getMessage());
        }
    }

    // Closes the connection
    public void close() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("Error closing DB connection: " + e.getMessage());
        }
    }
}

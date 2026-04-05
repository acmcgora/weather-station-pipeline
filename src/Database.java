package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

public class Database {

    private static final String URL = "jdbc:mysql://localhost:3306/weather";
    private static final String USER = "user";        // change if needed
    private static final String PASSWORD = "password"; // change to your real password

    public void save(double temperature) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);

            String sql = "INSERT INTO temperature (temperature, timestamp) VALUES (?, NOW())";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setDouble(1, temperature);
            stmt.executeUpdate();

            stmt.close();
            conn.close();

            System.out.println("Database has stored value: " + temperature + " °C");

        } catch (Exception e) {
            System.out.println("DB save failed."); // clean single line
        }
    }
}

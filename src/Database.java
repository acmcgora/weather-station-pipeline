package database;

import java.util.ArrayList;
import java.util.List;

public class Database {

    private final List<Double> records = new ArrayList<>();

    public void save(double temperature) {
        records.add(temperature);
        // Print with 1 decimal point
        System.out.println("Database has stored value: " + String.format("%.1f", temperature));
    }

    public int getRecordCount() {
        return records.size();
    }
}

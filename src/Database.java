package database;

import java.util.ArrayList;
import java.util.List;

public class Database {

    private final List<Double> records = new ArrayList<>();

    public void save(double temperature) {
        records.add(temperature);
        System.out.println("Database has stored value: " + temperature);
    }

    public int getRecordCount() {
        return records.size();
    }
}

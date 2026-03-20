import api.RestAPI;
import database.Database;
import sensor.Sensor;
import transformer.Transformer;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class Main {

    // 🔹 Send voltage to Node.js Sampler if available
    public static Double sendToNodeSampler(double voltage) {
        try {
            URL url = new URL("http://localhost:8080/sample");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String json = String.format(
                    "{ \"sensorId\": \"%s\", \"timestamp\": \"%s\", \"voltage\": %.2f }",
                    "sensor-001",
                    DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                    voltage
            );

            conn.getOutputStream().write(json.getBytes());

            Scanner scanner = new Scanner(conn.getInputStream());
            String response = scanner.useDelimiter("\\A").next();
            scanner.close();

            String key = "\"sampledVoltage\":";
            int start = response.indexOf(key) + key.length();
            int end = response.indexOf("}", start);
            return Double.parseDouble(response.substring(start, end));

        } catch (IOException e) {
            System.out.println("Node.js sampler not reachable, using local value.");
            return null; // fallback
        }
    }

    // 🔹 Send voltage to Flask Transformer if available
    public static Double sendToTransformer(double voltage) {
        try {
            URL url = new URL("http://localhost:5001/transform");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String json = String.format("{ \"voltage\": %.2f }", voltage);
            conn.getOutputStream().write(json.getBytes());

            Scanner scanner = new Scanner(conn.getInputStream());
            String response = scanner.useDelimiter("\\A").next();
            scanner.close();

            String key = "\"temperature\":";
            int start = response.indexOf(key) + key.length();
            int end = response.indexOf("}", start);

            return Double.parseDouble(response.substring(start, end));

        } catch (IOException e) {
            System.out.println("Flask transformer not reachable, using local conversion.");
            return null; // fallback
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Sensor sensor = new Sensor();
        Transformer transformer = new Transformer();
        RestAPI api = new RestAPI();
        Database database = new Database();

        System.out.println("Weather Station Pipeline Started...");

        String ciCyclesEnv = System.getenv("CI_CYCLES");
        int maxCycles = ciCyclesEnv != null ? Integer.parseInt(ciCyclesEnv) : -1;

        int cycles = 0;
        int sleepTime = (maxCycles > 0) ? 200 : 1000;

        System.out.println("CI_CYCLES=" + ciCyclesEnv + "  maxCycles=" + maxCycles);

        while (true) {
            try {
                // 🔹 Sensor reading
                double voltage = sensor.generateVoltage();
                System.out.println("Sensor voltage: " + voltage + " V");

                String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());

                // 🔹 Sampler: try Node.js first
                Double sampled = sendToNodeSampler(voltage);
                if (sampled == null) sampled = voltage;

                // 🔹 Transformer: try Flask first
                Double temperature = sendToTransformer(sampled);
                if (temperature == null) temperature = transformer.voltageToTemperature(sampled);

                temperature = Math.round(temperature * 10.0) / 10.0;//Rounding to 1 decimal point
                System.out.println("Temperature (C): " + temperature + " °C");

                // 🔹 JSON logging
                System.out.println("#JSON input { \"sensorId\": \"sensor-001\", \"timestamp\": \"" 
                        + timestamp + "\", \"voltage\": " + voltage + " }");
                System.out.println("#JSON output { \"sensorId\": \"sensor-001\", \"timestamp\": \"" 
                        + timestamp + "\", \"Temperature\": " + temperature + " }");

                // 🔹 API & Database
                try { api.send(temperature); } catch (Exception e) { System.out.println("API send failed."); }
                try { database.save(temperature); } catch (Exception e) { System.out.println("DB save failed."); }

            } catch (Exception e) {
                System.out.println("Unexpected pipeline error: " + e.getMessage());
            }

            Thread.sleep(sleepTime);

            cycles++;
            if (maxCycles > 0 && cycles >= maxCycles) {
                System.out.println("Max cycles reached. Exiting...");
                break;
            }
        }
    }
}

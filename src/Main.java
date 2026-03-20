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
            return null; // fallback
        }
    }

    // 🔹 Run a single cycle of the pipeline
    private static void runCycle(Sensor sensor, Transformer transformer, RestAPI api, Database database, boolean allowExternal) {
        double voltage = sensor.generateVoltage();
        System.out.println("Sensor voltage: " + voltage + " V");

        String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());

        // Sample
        Double sampled = allowExternal ? sendToNodeSampler(voltage) : null;
        if (sampled != null) {
            System.out.println("Using EXTERNAL Node.js sampler");
        } else {
            sampled = voltage;
            System.out.println("Using LOCAL sampler");
        }

        // Transform
        Double temperature = allowExternal ? sendToTransformer(sampled) : null;
        if (temperature != null) {
            System.out.println("Using EXTERNAL Flask transformer");
        } else {
            temperature = transformer.voltageToTemperature(sampled);
            System.out.println("Using LOCAL Java transformer");
        }

        // Round to 1 decimal place
        temperature = Math.round(temperature * 10.0) / 10.0;
        System.out.println("Temperature (C): " + temperature + " °C");

        // JSON logging
        System.out.println("#JSON input { \"sensorId\": \"sensor-001\", \"timestamp\": \"" 
                + timestamp + "\", \"voltage\": " + voltage + " }");
        System.out.println("#JSON output { \"sensorId\": \"sensor-001\", \"timestamp\": \"" 
                + timestamp + "\", \"Temperature\": " + temperature + " }");

        // API & Database
        try { api.send(temperature); } catch (Exception e) { System.out.println("API send failed."); }
        try { database.save(temperature); } catch (Exception e) { System.out.println("DB save failed."); }
    }

    public static void main(String[] args) throws InterruptedException {
        Sensor sensor = new Sensor();
        Transformer transformer = new Transformer();
        RestAPI api = new RestAPI();
        Database database = new Database();

        System.out.println("Weather Station Pipeline Started...");

        // Use environment variable for CI cycles, default to 5
        String ciCyclesEnv = System.getenv("CI_CYCLES");
        int totalCycles = ciCyclesEnv != null ? Integer.parseInt(ciCyclesEnv) : 5;
        int phaseCycles = totalCycles / 2; // half for external, half for local
        int sleepTime = 200;

        System.out.println("Total CI cycles: " + totalCycles + " (External: " + phaseCycles + ", Local: " + phaseCycles + ")");

        //  External services if available 
        System.out.println("\n External Services");
        for (int i = 0; i < phaseCycles; i++) {
            runCycle(sensor, transformer, api, database, true);
            Thread.sleep(sleepTime);
        }

        // Phase 2: Force local 
        System.out.println("\n Local Services");
        for (int i = 0; i < totalCycles - phaseCycles; i++) {
            runCycle(sensor, transformer, api, database, false);
            Thread.sleep(sleepTime);
        }

        System.out.println("\nAll cycles completed. Exiting...");
    }
}

import api.RestAPI;
import database.Database;
import sensor.Sensor;
import transformer.Transformer;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

public class Main {

    // Sends voltage to the Python Flask Transformer and gets temperature
    public static double sendToFlaskTransformer(double voltage) throws Exception {
        URL url = new URL("http://localhost:5001/transform");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        String json = String.format("{\"voltage\": %.2f}", voltage);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes());
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();

        String response = sb.toString();
        // Parse JSON response {"temperature": value}
        String key = "\"temperature\":";
        int start = response.indexOf(key) + key.length();
        int end = response.indexOf("}", start);
        return Double.parseDouble(response.substring(start, end).trim());
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

        while (true) {
            try {
                // Sensor reading
                double voltage = sensor.generateVoltage();
                System.out.printf("Sensor voltage: %.2f V%n", voltage);

                String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());

                // Transformer: switch between CI (local Java) or Flask (HTTP)
                double temperature;
                if (maxCycles > 0) {
                    // CI environment: use local Java transformer
                    System.out.println("CI detected — using local transformer.");
                    temperature = transformer.voltageToTemperature(voltage);
                } else {
                    // Local/dev: use Flask service
                    System.out.println("Using Flask Transformer service...");
                    try {
                        temperature = sendToFlaskTransformer(voltage);
                    } catch (Exception e) {
                        System.out.println("Flask Transformer unavailable — falling back to local transformer.");
                        temperature = transformer.voltageToTemperature(voltage);
                    }
                }

                System.out.printf("Temperature (C): %.2f%n", temperature);

                // JSON input/output logging
                String jsonInput = String.format(
                        "{ \"sensorId\": \"%s\", \"timestamp\": \"%s\", \"voltage\": %.2f }",
                        "sensor-001", timestamp, voltage
                );
                String jsonOutput = String.format(
                        "{ \"sensorId\": \"%s\", \"timestamp\": \"%s\", \"sampledVoltage\": %.2f }",
                        "sensor-001", timestamp, temperature
                );
                System.out.println("#JSON input " + jsonInput);
                System.out.println("#JSON output " + jsonOutput);

                // REST API send
                try { api.send(temperature); } catch (Exception e) {
                    System.out.println("API send failed. Retrying...");
                    try { api.send(temperature); } catch (Exception ignored) {
                        System.out.println("API send failed again. Degrading service.");
                    }
                }

                // Database save
                try { database.save(temperature); } catch (Exception e) {
                    System.out.println("DB save failed. Retrying...");
                    try { database.save(temperature); } catch (Exception ignored) {
                        System.out.println("DB save failed again. Continuing without persistence.");
                    }
                }

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

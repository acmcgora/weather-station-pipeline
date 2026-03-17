import api.RestAPI;
import database.Database;
import sensor.Sensor;
import transformer.Transformer;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

public class Main {

    // Node.js Sampler call (skipped in CI)
    public static double sendToNodeSampler(double voltage) throws Exception {
        java.net.URL url = new java.net.URL("http://localhost:8080/sample");
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        String json = String.format(
                "{ \"sensorId\": \"%s\", \"timestamp\": \"%s\", \"voltage\": %.2f }",
                "sensor-001",
                DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                voltage
        );

        try (java.io.OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes());
        }

        java.util.Scanner scanner = new java.util.Scanner(conn.getInputStream());
        String response = scanner.useDelimiter("\\A").next();
        scanner.close();

        String key = "\"sampledVoltage\":";
        int start = response.indexOf(key) + key.length();
        int end = response.indexOf("}", start);
        return Double.parseDouble(response.substring(start, end));
    }

    public static void main(String[] args) throws InterruptedException {

        Sensor sensor = new Sensor();
        Transformer transformer = new Transformer();
        RestAPI api = new RestAPI();
        Database database = new Database();

        System.out.println("Weather Station Pipeline Started...");

        // Detect CI environment
        String ciCyclesEnv = System.getenv("CI_CYCLES");
        int maxCycles = ciCyclesEnv != null ? Integer.parseInt(ciCyclesEnv) : -1;

        int cycles = 0;
        int sleepTime = (maxCycles > 0) ? 200 : 1000; // faster in CI

        System.out.println("CI_CYCLES=" + ciCyclesEnv + "  maxCycles=" + maxCycles);

        while (true) {
            try {
                double voltage = sensor.generateVoltage();

                // Use ISO 8601 timestamp
                String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());

                // CI-safe sampler
                double sampled;
                if (maxCycles > 0) {
                    sampled = voltage; // skip Node.js in CI
                    System.out.println("CI detected — skipping Node.js sampler.");
                } else {
                    sampled = sendToNodeSampler(voltage); // local run
                }

                // Transformer with recovery
                double temperature;
                try {
                    temperature = transformer.voltageToTemperature(sampled);
                } catch (Exception e) {
                    System.out.println("Transformer failure detected. Restarting transformer...");
                    transformer = new Transformer();
                    Thread.sleep(3000);
                    continue;
                }

                // JSON Input log
                String jsonInput = String.format(
                        "{ \"sensorId\": \"%s\", \"timestamp\": \"%s\", \"voltage\": %.2f }",
                        "sensor-001", timestamp, voltage
                );
                System.out.println("#JSON input " + jsonInput);

                // JSON Output log
                String jsonOutput = String.format(
                        "{ \"sensorId\": \"%s\", \"timestamp\": \"%s\", \"sampledVoltage\": %.2f }",
                        "sensor-001", timestamp, temperature
                );
                System.out.println("#JSON output " + jsonOutput);

                // API send with retry
                try {
                    api.send(temperature);
                } catch (Exception e) {
                    System.out.println("API send failed. Retrying...");
                    try { api.send(temperature); } catch (Exception ignored) {
                        System.out.println("API send failed again. Degrading service.");
                    }
                }

                // Database save with retry
                try {
                    database.save(temperature);
                } catch (Exception e) {
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

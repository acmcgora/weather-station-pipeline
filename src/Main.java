import api.RestAPI;
import database.Database;
import sensor.Sensor;
import transformer.Transformer;

import org.json.JSONObject;

public class Main {

    public static double sendToNodeSampler(double voltage) throws Exception {
        // Local run only — CI will skip this
        java.net.URL url = new java.net.URL("http://localhost:8080/sample");
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        String json = String.format(
                "{\"sensorId\":\"sensor-001\",\"timestamp\":\"%d\",\"voltage\":%f}",
                System.currentTimeMillis(),
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

                // CI-safe sampler
                double sampled;
                if (maxCycles > 0) {
                    sampled = voltage;
                    System.out.println("CI detected — skipping Node.js sampler.");
                } else {
                    sampled = sendToNodeSampler(voltage);
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

                // JSON output for logs
                JSONObject jsonLog = new JSONObject();
                jsonLog.put("sensorId", "sensor-001");
                jsonLog.put("voltage", voltage);
                jsonLog.put("sampledVoltage", sampled);
                jsonLog.put("temperatureC", temperature);
                System.out.println(jsonLog.toString());

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

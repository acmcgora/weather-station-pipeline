import api.RestAPI;
import database.Database;
import sensor.Sensor;
import transformer.Transformer;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;


public class Main {
    public static double sendToNodeSampler(double voltage) throws Exception {
            URL url = new URL("http://localhost:8080/sample");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
        
            String json = String.format(
                "{\"sensorId\":\"sensor-001\",\"timestamp\":\"%d\",\"voltage\":%f}",
                System.currentTimeMillis(),
                voltage
            );
        
            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes());
            }
        
            Scanner scanner = new Scanner(conn.getInputStream());
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

        // Check for CI environment variable
        String ciCyclesEnv = System.getenv("CI_CYCLES");
        int maxCycles = ciCyclesEnv != null ? Integer.parseInt(ciCyclesEnv) : -1;

        int cycles = 0;

        // Use shorter sleep for CI
        int sleepTime = (maxCycles > 0) ? 200 : 1000; // 200ms per iteration in CI, 1s locally

        System.out.println("CI_CYCLES=" + ciCyclesEnv + "  maxCycles=" + maxCycles);

        while (true) {
            try {
                double voltage = sensor.generateVoltage();
                 double sampled;
                if (maxCycles > 0) { // CI run detected
                    sampled = voltage; // skip Node.js call
                    System.out.println("CI detected — skipping Node.js sampler.");
                } else {
                    sampled = sendToNodeSampler(voltage); // local run
                }

                double temperature;
                try {
                    temperature = transformer.voltageToTemperature(sampled);
                } catch (Exception e) {
                    System.out.println("Transformer failure detected. Restarting transformer...");
                    transformer = new Transformer();   // restart component
                    Thread.sleep(3000);                // recovery window
                    continue;                          // skip this cycle
                } 
                try {
                    api.send(temperature);
                } catch (Exception e) { // Retry API send
                    System.out.println("API send failed. Retrying...");
                    try { api.send(temperature); } catch (Exception ignored) {
                        System.out.println("API send failed again. Degrading service.");
                    }
                }

                try {
                    database.save(temperature);
                } catch (Exception e) {
                    System.out.println("DB save failed. Retrying...");
                    try { database.save(temperature); } catch (Exception ignored) { // Retry DB save 
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


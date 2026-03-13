import api.RestAPI;
import database.Database;
import sampler.Sampler;
import sensor.Sensor;
import transformer.Transformer;

public class Main {

    public static void main(String[] args) throws InterruptedException {

        Sensor sensor = new Sensor();
        Sampler sampler = new Sampler();
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
                double sampled = sendToNodeSampler(voltage);

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

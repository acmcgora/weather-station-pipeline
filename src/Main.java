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
            double voltage = sensor.generateVoltage();
            double sampled = sampler.sample(voltage);
            double temperature = transformer.voltageToTemperature(sampled);
            api.send(temperature);
            database.save(temperature);

            Thread.sleep(sleepTime);

            cycles++;
            if (maxCycles > 0 && cycles >= maxCycles) {
                System.out.println("Max cycles reached. Exiting...");
                break;
            }
        }
    }
}
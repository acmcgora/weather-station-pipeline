import sensor.Sensor;
import sampler.Sampler;
import transformer.Transformer;
import api.RestAPI;
import database.Database;

public class Main {

    public static void main(String[] args) throws InterruptedException {

        Sensor sensor = new Sensor();
        Sampler sampler = new Sampler();
        Transformer transformer = new Transformer();
        RestAPI api = new RestAPI();
        Database database = new Database();

        System.out.println("Weather Station Pipeline Started...");

        while (true) {
            double voltage = sensor.generateVoltage();
            System.out.println("Sensor voltage: " + voltage + " V");

            double sampled = sampler.sample(voltage);

            double temperature = transformer.voltageToTemperature(sampled);

            api.send(temperature);

            database.save(temperature);

            Thread.sleep(1000);
        }
    }
}

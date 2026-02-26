package sensor;

import java.util.Random;

public class Sensor {

    private final Random random = new Random();

    public double generateVoltage() {
        return Math.round(random.nextDouble() * 5.0 * 100.0) / 100.0;
    }
}

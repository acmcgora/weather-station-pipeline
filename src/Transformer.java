package transformer;

public class Transformer {

    /**
     * Converts sampled voltage to temperature (0-100°C)
     * and returns it as a JSON string.
     */
    public String voltageToTemperatureJSON(double voltage) {
        if (voltage < 0 || voltage > 5) {
            throw new IllegalArgumentException("Voltage out of range: 0-5V allowed");
        }

        double temperature = (voltage / 5.0) * 100.0;

        // Return JSON string without any library
        return String.format("{\"temperature\": %.2f}", temperature);
    }
}

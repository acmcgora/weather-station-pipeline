package transformer;

public class Transformer {

    /**
     * Converts sampled voltage (0–5V) to temperature (0–100°C)
     * and returns as a double.
     */
    public double voltageToTemperature(double voltage) {
        if (voltage < 0 || voltage > 5) {
            throw new IllegalArgumentException("Voltage out of range: 0–5V allowed");
        }
        return (voltage / 5.0) * 100.0;
    }

    /**
     * Converts sampled voltage to temperature and returns JSON string.
     */
    public String voltageToTemperatureJSON(double voltage) {
        double temperature = voltageToTemperature(voltage);
        return String.format("{\"temperature\": %.2f}", temperature);
    }
}

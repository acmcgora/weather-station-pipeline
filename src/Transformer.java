package transformer;

public class Transformer {

    /**
     * Converts sampled voltage (0–5V) to temperature (0–100°C)
     * and returns as a double rounded to 1 decimal.
     */
    public double voltageToTemperature(double voltage) {
        if (voltage < 0 || voltage > 5) {
            throw new IllegalArgumentException("Voltage out of range: 0–5V allowed");
        }
        double temp = (voltage / 5.0) * 100.0;
        return Math.round(temp * 10.0) / 10.0;  // round to 1 decimal
    }

    /**
     * Converts sampled voltage to temperature and returns JSON string
     * with 1 decimal for consistency.
     */
    public String voltageToTemperatureJSON(double voltage) {
        double temperature = voltageToTemperature(voltage);
        return String.format("{\"temperature\": %.1f}", temperature);
    }
}

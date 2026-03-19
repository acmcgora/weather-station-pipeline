package api;

public class RestAPI {

    public void send(double temperature) {
       double rounded = Math.round(temperature * 10.0) / 10.0;
        System.out.println("REST API received temperature: " + rounded + " °C");
    }
}

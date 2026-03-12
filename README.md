# weather-station-pipeline
The Pipeline runs like this:
Sensor → Sampler → Transformer → REST API → Database

| Module      | Description |                                              
| Sensor      | Generates simulated voltage readings from the environment 
| Sampler     | Samples the sensor voltage values                         
| Transformer | Converts voltage values to temperature readings           
| REST API    | Receives temperature data and simulates an API endpoint   
| Database    | Stores the processed temperature values                   


#JSON input
{
  "sensorId": "sensor-001",
  "timestamp": "2026-03-12T10:15:30Z",
  "voltage": 3.12
}

#JSON output
{
  "sensorId": "sensor-001",
  "timestamp": "2026-03-12T10:15:30Z",
  "temperatureC": 62.4
}

The Sensor generates raw voltage data, the Sampler collects the readings, the Transformer converts voltage into temperature values, the REST API simulates receiving processed data, and the Database stores the final results. Keeping these separate can help mitigate the damage in the event of a fault.

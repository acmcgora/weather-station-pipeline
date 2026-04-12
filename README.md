The pipeline runs like this:

Sensor → Sampler → Transformer → REST API → Database

Module	Description
Sensor	Generates simulated voltage readings from the environment
Sampler	Samples the sensor voltage values
Transformer	Converts voltage values to temperature readings
REST API	Receives temperature data and simulates an API endpoint
Database	Stores the processed temperature values

Transformer API
Endpoint:
POST http://localhost:5001/transform

Input JSON Example:
{
  "voltage": 3.12
}
Output JSON Example:
{
  "temperature": 62.4
}
How it works:
The Transformer receives a sampled voltage value from the Sampler.
It converts the voltage to a temperature using a simple linear formula:
temperature (C) = (voltage / 5.0) * 100

The result is returned in JSON format, making it easy pipeline
Using JSON over HTTP allows the Transformer to run independently, improving modifiability and integration.

Sample JSON Flow
JSON Input (to Sampler):
{ 
  "sensorId": "sensor-001", 
  "timestamp": "2026-03-12T10:15:30Z", 
  "voltage": 3.12 
}
JSON Output (from Transformer / after sampling):
{ 
  "sensorId": "sensor-001", 
  "timestamp": "2026-03-12T10:15:30Z", 
  "sampledVoltage": 62.4 
}
The Sensor generates raw voltage data.
The Sampler collects the readings.
The Transformer converts voltage into temperature values.
The REST API simulates receiving processed data.
The database stores final results locally using MySQL. It is not used in the GitHub Actions pipeline because the CI environment does not include a database service or JDBC driver to connect from actions, so only local executions of the program will keep the data.

Keeping these modules separate helps lessen the impact of faults in one module and allows independent testing and maintenance.

### Assignment 4 - REST API Design and Implementation
#### Endpoint
POST /temperature

#### Input JSON Example
{
  "temperature_c": 24.5,
  "timestamp": "2026-03-10T14:20:00Z"
}

#### Output JSON Example
{
  "status": "stored",
  "id": 104,
  "temperature_c": 24.5,
  "timestamp": "2026-03-10T14:20:00Z"
}

#### Design Explanation
The REST API uses a single POST endpoint to keep the interface simple and focused on receiving temperature readings from the Transformer. We used JSON because it's human‑readable and easy for both Python and Java components to generate. The API validates incoming data, assigns a timestamp when one is missing, and returns a confirmation response so services can verify that it stored correctly. This design keeps the service predictable and easy to integrate into the pipeline.

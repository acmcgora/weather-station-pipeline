from flask import Flask, request, jsonify

app = Flask(__name__)

# Conversion function
def voltage_to_temperature(voltage):
    return (voltage / 5.0) * 100.0

@app.route('/transform', methods=['POST'])
def transform():
    data = request.get_json()
    print(f"[Transformer] Received data: {data}")  # Log incoming request

    # Basic error handling
    if not data or 'voltage' not in data:
        print("[Transformer] Error: Invalid input")
        return jsonify({"error": "Invalid input"}), 400

    try:
        voltage = float(data['voltage'])
    except ValueError:
        print("[Transformer] Error: Voltage must be numeric")
        return jsonify({"error": "Voltage must be numeric"}), 400

    # Convert voltage to temperature
    temperature = voltage_to_temperature(voltage)
    print(f"[Transformer] Converted voltage {voltage} V -> {temperature:.2f} °C")

    # Return JSON response
    return jsonify({
        "temperature": temperature
    })

if __name__ == '__main__':
    print("[Transformer] Starting Flask app on port 5001...")
    app.run(host='0.0.0.0', port=5001)

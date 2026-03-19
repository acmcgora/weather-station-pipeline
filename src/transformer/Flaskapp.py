from flask import Flask, request, jsonify

app = Flask(__name__)

# Conversion function
def voltage_to_temperature(voltage):
    return (voltage / 5.0) * 100.0

@app.route('/transform', methods=['POST'])
def transform():
    data = request.get_json()

    # Basic error handling
    if not data or 'voltage' not in data:
        return jsonify({"error": "Invalid input"}), 400

    try:
        voltage = float(data['voltage'])
    except ValueError:
        return jsonify({"error": "Voltage must be numeric"}), 400

    # Convert voltage to temperature
    temperature = voltage_to_temperature(voltage)

    # Return JSON response
    return jsonify({
        "temperature": temperature
    })

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5001)

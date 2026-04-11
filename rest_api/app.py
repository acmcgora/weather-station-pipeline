from flask import Flask, request, jsonify
from datetime import datetime
import os
import mysql.connector

app = Flask(__name__)

def get_db():
    return mysql.connector.connect(
        host=os.getenv("DB_HOST", "db"),
        user=os.getenv("DB_USER", "user"),
        password=os.getenv("DB_PASSWORD", "password"),
        database=os.getenv("DB_NAME", "weather")
    )

@app.post("/temperature")
def temperature():
    data = request.get_json()

    if not data or "temperature_c" not in data:
        return jsonify({"status": "error", "message": "temperature_c required"}), 400

    temp = float(data["temperature_c"])
    ts = data.get("timestamp", datetime.utcnow().isoformat())

    conn = get_db()
    cur = conn.cursor()

    cur.execute(
        "INSERT INTO temperature (temperature, timestamp) VALUES (%s, %s)",
        (temp, ts)
    )
    conn.commit()

    return jsonify({
        "status": "stored",
        "id": cur.lastrowid,
        "temperature_c": temp,
        "timestamp": ts
    }), 201
 

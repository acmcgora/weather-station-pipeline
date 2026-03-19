import pytest
from src.transformer.Flaskapp import app

@pytest.fixture
def client():
    return app.test_client()

def test_voltage_to_temperature(client):
    response = client.post('/transform', json={"voltage": 2.5})
    data = response.get_json()
    assert response.status_code == 200
    assert data['temperature'] == 50.0

def test_invalid_input(client):
    response = client.post('/transform', json={})
    data = response.get_json()
    assert response.status_code == 400
    assert "error" in data

import sys
import os
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '../src/transformer')))
#Imports Flask app
from Flaskapp import app
import pytest

@pytest.fixture
def client():
    return app.test_client()

def test_voltage_to_temperature(client):
     """
    Test the /transform endpoint with a valid voltage.
    Expect the temperature to be calculated correctly.
    """
    response = client.post('/transform', json={"voltage": 2.5})
    data = response.get_json()
# Check HTTP response code
    assert response.status_code == 200
#Make sure the temp is correct
    assert data['temperature'] == 50.0

def test_invalid_input(client):
    """
    Test the /transform endpoint with invalid input (missing voltage field).
    Expect the API to return a 400 status code and an error message.
    """
    response = client.post('/transform', json={})
    data = response.get_json()
    #Check HTTP response code
    assert response.status_code == 400
    #Make sure the response give back a error
    assert "error" in data

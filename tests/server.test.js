const request = require('supertest');
const app = require('../src/sampler/Node.js');

describe('POST /sample', () => {
  test('returns sampled voltage for valid input', async () => {
    const res = await request(app)
      .post('/sample')
      .send({
        sensorId: 'sensor-123',
        timestamp: '2025-03-12T17:10:00Z',
        voltage: 3.7
      });

    expect(res.statusCode).toBe(200);
    expect(res.body.sensorId).toBe('sensor-123');
    expect(res.body.timestamp).toBe('2025-03-12T17:10:00Z');
    expect(res.body.sampledVoltage).toBe(3.7);
  });

  test('returns null fields when missing input (code does not validate)', async () => {
    const res = await request(app)
      .post('/sample')
      .send({});

    expect(res.statusCode).toBe(200);
    expect(res.body).toEqual({
      sensorId: undefined,
      timestamp: undefined,
      sampledVoltage: undefined
    });
  });
});

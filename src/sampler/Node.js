const express = require('express');
const app = express();
const port = 8080;

app.use(express.json());

app.post('/sample', (req, res) => {
  const { sensorId, timestamp, voltage } = req.body;

  const sampled = {
    sensorId,
    timestamp,
    sampledVoltage: voltage
  };

  res.json(sampled);
});

//app.listen(port, () => {
//  console.log(`Sampler running on port ${port}`);
//});
if (require.main === module) {
  app.listen(port, () => {
    console.log(`Sampler running on port ${port}`);
  });
}

module.exports = app;

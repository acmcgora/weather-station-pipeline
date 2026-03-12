const express = require('express');
const app = express();

app.use(express.json());

app.post('/sample', (req, res) => {
  const data = req.body;

  // Check for required fields
  if (!data.temperature) {
    return res.status(400).json({ error: "temperature is required" });
  }

  if (!data.timestamp) {
    return res.status(400).json({ error: "timestamp is required" });
  }

  // Send back a simple response
  res.json({
    message: "Sample received",
    received: data
  });
});

app.listen(3000, () => {
  console.log("Sampler running on port 3000");
});

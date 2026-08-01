const { askAI } = require("./aiServices");

async function detectIntent(question) {

    const prompt = `
You are an intent classifier for a hospital queue assistant.

Return ONLY valid JSON.

Format:

{
  "intent": "QUEUE | DOCTOR | HOSPITAL | GENERAL",
  "filters": {}
}

Examples:

Question:
When is my turn?

Response:
{
  "intent": "QUEUE",
  "filters": {}
}

Question:
Who is treating me?

Response:
{
  "intent": "DOCTOR",
  "filters": {
    "assignedDoctor": true
  }
}

Question:
Is any heart doctor available?

Response:
{
  "intent": "DOCTOR",
  "filters": {
    "specialization": "heart"
  }
}

Question:
Is Dr Ravi available today?

Response:
{
  "intent": "DOCTOR",
  "filters": {
    "doctorName": "Ravi"
  }
}

Question:
Show all doctors

Response:
{
  "intent": "DOCTOR",
  "filters": {}
}

Question:
What is the hospital address?

Response:
{
  "intent": "HOSPITAL",
  "filters": {}
}

Question:
Hello

Response:
{
  "intent": "GENERAL",
  "filters": {}
}

Question:
${question}
`;

    const response = await askAI(prompt);

    try {
        return JSON.parse(response);
    } catch (err) {
        return {
            intent: "GENERAL",
            filters: {}
        };
    }
}

module.exports = {
    detectIntent
};
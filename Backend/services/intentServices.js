const { askAI } = require("./aiServices");

async function detectIntent(question) {

  const prompt = `
You are an intent classifier for a hospital queue assistant.

Your job is to classify the user's question into EXACTLY ONE of these intents:

- MY_QUEUE: Questions about the user's own queue, token, wait time, ETA, status, or turn.
- QUEUE_COMPARISON: Questions comparing departments or queues, busiest queue, shortest wait, etc.
- DOCTOR: Questions about doctors, availability, specialization, or assigned doctor.
- HOSPITAL: Questions about hospital information like address, phone number, timings.
- GENERAL: Greetings or anything unrelated to the above.

Return ONLY valid JSON in this format:

{
  "intent": "<INTENT>",
  "filters": {}
}

Examples:

Question: "When is my turn?"
Response:
{"intent":"MY_QUEUE","filters":{}}

Question: "What is my status?"
Response:
{"intent":"MY_QUEUE","filters":{}}

Question: "Can I leave for 10 minutes?"
Response:
{"intent":"MY_QUEUE","filters":{}}

Question: "Which department is busy?"
Response:
{"intent":"QUEUE_COMPARISON","filters":{}}

Question: "Which queue has the shortest wait?"
Response:
{"intent":"QUEUE_COMPARISON","filters":{}}

Question: "Show all doctors."
Response:
{"intent":"DOCTOR","filters":{}}

Question: "Is Dr Ravi available today?"
Response:
{"intent":"DOCTOR","filters":{"doctorName":"Ravi"}}

Question: "What is the hospital address?"
Response:
{"intent":"HOSPITAL","filters":{}}

Question: "Hello"
Response:
{"intent":"GENERAL","filters":{}}

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
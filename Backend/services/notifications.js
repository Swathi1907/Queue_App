const { askAI } = require("./aiServices");

async function generateNotification(data) {

    const prompt = `
You are QueueAI, an AI assistant for a hospital queue management app.

Generate a push notification.

Current Situation:
- Notification Type: ${data.type}
- Department: ${data.queueName}
- Token: ${data.token || "N/A"}
- People Ahead: ${data.peopleAhead ?? "N/A"}
- Average Service Time: ${data.avgServiceTime ?? "N/A"} minutes

Notification Types:
- TURN: The user's token is now being served.
- READY_1: The user is next in the queue.
- READY_2: One patient remains before the user.
- QUEUE_PAUSED: The doctor has temporarily paused the queue.
- QUEUE_RESUMED: The queue has resumed.
- QUEUE_CLOSED: The queue has closed for the day.

Rules:
- Title should be short (2-5 words).
- Message should be clear and actionable.
- Mention the department.
- Be friendly and reassuring.
- Maximum 20 words.
- Return ONLY valid JSON.

Format:
{
  "title": "...",
  "message": "..."
}
`;

    const response = await askAI(prompt);

    return JSON.parse(response);
}

module.exports = {
    generateNotification
};
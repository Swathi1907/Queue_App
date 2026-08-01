const Groq = require("groq-sdk");

const groq = new Groq({
    apiKey: process.env.GROQ_API_KEY
});

async function askAI(prompt) {

    const response =
        await groq.chat.completions.create({

            model: "llama-3.3-70b-versatile",

            messages: [

                {
                    role: "system",
                    content: "You are QueueAI, an intelligent hospital assistant."
                },

                {
                    role: "user",
                    content: prompt
                }

            ]

        });

    return response.choices[0].message.content;

}

module.exports = {
    askAI
};
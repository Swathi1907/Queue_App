const express= require('express');

const route = express.Router();

const Groq = require("groq-sdk");
const {buildQueueContext}=require('../services/queueServices')
const Authmiddleware = require("../middleware/authmiddleware");
const { askAI } =
require("../services/aiServices");
const { detectIntent } =
require("../services/intentServices");
const { buildDoctorContext } =
require("../services/doctorServices");
route.post("/chat",Authmiddleware, async (req, res) => {

    try {

        const { question } = req.body;
       const result = await detectIntent(question);

const intent = result.intent;
const filters = result.filters;
console.log(intent);
let context = "";
switch (intent) {

    case "QUEUE":

        context = await buildQueueContext(req.user.userId);

        if (!context) {
            return res.json({
                answer: "You are not currently in any active queue."
            });
        }

        break;

    case "DOCTOR":

        context = await buildDoctorContext(req.user.userId, filters);

        if (!context) {
            return res.json({
                answer: "No doctor information found."
            });
        }

        break;

  //  case "HOSPITAL":

      //  context = await buildHospitalContext(...);

     //   if (!context) {
         //   return res.json({
      //          answer: "Hospital information not found."
        //    });
       

        break;
}
    
if (intent !== "GENERAL" && !context) {

    return res.json({
        answer: "You are not currently in any active queue."
    });

}
const prompt = `
You are QueueAI, an AI assistant for a hospital queue management app.

The user may have multiple active queues.
Context:

${context}

User Question:
${question}
Rules:
- Answer only using the information provided.
- Never invent information.
- If the user has multiple queues and the question is ambiguous, ask which hospital or department they mean.
- If the user asks why their token number is higher than expected, use "Tokens Before You" to explain whether earlier tokens were completed, cancelled, serving, or waiting.
- Token numbers are never reassigned after they are issued.
- If information is unavailable, clearly say so.
- Keep answers under 60 words.
- Be polite and reassuring.`;


       const answer =
await askAI(prompt);

res.json({
    answer
});

    } catch (err) {

        res.status(500).json({

            message: err.message

        });

    }

});

module.exports = route;
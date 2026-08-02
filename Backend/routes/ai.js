const express= require('express');

const route = express.Router();

const Groq = require("groq-sdk");
const {buildQueueContext}=require('../services/queueServices');
const {buildQueueComparisions}=require('../services/queuecomparisions')
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

    case "MY_QUEUE":

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
case "QUEUE_COMPARISON":

    context = await buildQueueComparisions(req.user.userId, filters);

    if (!context || context.length === 0) {
        return res.json({
            answer: "No active queues found."
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

}let prompt = "";

if (intent === "QUEUE_COMPARISON") {

    prompt = `
You are QueueAI, an AI assistant for a hospital queue management app.

Current queue information across all hospitals:

${context.map(q => `
Hospital: ${q.hospitalName}
Department: ${q.department}
Current Waiting Patients: ${q.waitingPatients}
Average Service Time Per Patient: ${q.avgServiceTime} minutes
Estimated Total Waiting Time: ${q.estimatedWait} minutes
Queue Status: ${q.status}
`).join("\n")}

User Question:
${question}

Rules:
- Use ONLY the information provided.
- Never invent facts.
- Compare hospitals or departments when asked.
- If asked which department is busiest, consider both the number of waiting patients and the estimated waiting time.
- If asked which queue is fastest, recommend the one with the lowest estimated waiting time.
- Keep the answer under 60 words.
`;
} else {

    prompt = `
You are QueueAI, an AI assistant for a hospital queue management app.

Context:

${context}

User Question:
${question}

Rules:
- Answer only using the information provided.
- Never invent information.
- Keep answers under 60 words.
`;
}
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
const express = require("express");
const router = express.Router();

const { getMessaging } = require("firebase-admin/messaging");

router.post("/", async (req, res) => {
    try {
        console.log("🔥 ROUTE HIT");
        console.log(req.body);

        const { token } = req.body;

        console.log("TOKEN:", token);

        const response = await getMessaging().send({
            token,
            notification: {
                title: "Queue App",
                body: "Hello Swathi 🎉"
            }
        });

        console.log("MESSAGE ID:", response);

        res.json({
            message: "Notification sent"
        });

    } catch (err) {
        console.error("FCM ERROR:", err);

        res.status(500).json({
            error: err.message
        });
    }
});

module.exports = router;
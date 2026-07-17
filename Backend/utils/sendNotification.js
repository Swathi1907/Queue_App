const { getMessaging } = require("firebase-admin/messaging");

async function sendNotification(token, title, body) {

    if (!token) return;

    await getMessaging().send({
        token,
        notification: {
            title,
            body
        }
    });

}

module.exports = sendNotification;
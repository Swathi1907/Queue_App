const { initializeApp, cert } = require("firebase-admin/app");

const serviceAccount = require("./firebase-service-account.json");

initializeApp({
    credential: cert(serviceAccount),
});
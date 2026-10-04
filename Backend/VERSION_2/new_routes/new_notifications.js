const express = require("express");
const router = express.Router();

const notificationController =
    require("../new_controllers/notification_controller");
const { authmiddleware, authorize } = require('../new_middleware/authmiddleware'); 


console.log("================================");
console.log("NOTIFICATION ROUTE DEBUG");
console.log(
    "Authmiddleware:",
    typeof Authmiddleware
);
console.log(
    "notificationController:",

    notificationController
);
console.log(
    "getNotifications:",
    typeof notificationController.getNotifications
);
console.log("================================");

router.get(
    "/",
    authmiddleware,
    notificationController.getNotifications
);

module.exports = router;
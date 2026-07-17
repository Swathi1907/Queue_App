const express = require("express");
const router = express.Router();

const Notification = require("../models/notifications_model");
const Authmiddleware = require("../middleware/Authmiddleware");


// Get Today's Notifications
router.get("/", Authmiddleware, async (req, res) => {

    try {

        const today = new Date();
        today.setHours(0, 0, 0, 0);

        const notifications = await Notification.find({
            userId: req.user.userId,
            createdAt: { $gte: today }
        }).sort({ createdAt: -1 });

        res.status(200).json(notifications);

    } catch (err) {

        res.status(500).json({
            message: err.message
        });

    }

});


// Mark all notifications as read
router.put("/read", Authmiddleware, async (req, res) => {

    try {

        await Notification.updateMany(
            {
                userId: req.user.userId,
                isRead: false
            },
            {
                $set: {
                    isRead: true
                }
            }
        );

        res.status(200).json({
            message: "Notifications marked as read."
        });

    } catch (err) {

        res.status(500).json({
            message: err.message
        });

    }

});

router.get("/count", Authmiddleware, async (req, res) => {

    try {

        const count = await Notification.countDocuments({

            userId: req.user.userId,
            isRead: false

        });

        res.status(200).json({

            count

        });

    } catch (err) {

        res.status(500).json({

            message: err.message

        });

    }

});
// Clear today's notifications (Optional)
router.delete("/today", Authmiddleware, async (req, res) => {

    try {

        const today = new Date();
        today.setHours(0, 0, 0, 0);

        await Notification.deleteMany({
            userId: req.user.userId,
            createdAt: { $gte: today }
        });

        res.status(200).json({
            message: "Today's notifications cleared."
        });

    } catch (err) {

        res.status(500).json({
            message: err.message
        });

    }

});

module.exports = router;
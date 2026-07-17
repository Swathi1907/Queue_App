const mongoose = require("mongoose");

const NotificationSchema = new mongoose.Schema(
    {
        userId: {
            type: mongoose.Schema.Types.ObjectId,
            ref: "User",
            required: true
        },

        queueId: {
            type: mongoose.Schema.Types.ObjectId,
            ref: "Queue",
            required: true
        },

        title: {
            type: String,
            required: true
        },

        message: {
            type: String,
            required: true
        },

        type: {
            type: String,
            enum: [
                "JOIN",
                "READY_2",
                "READY_1",
                "TURN",
                "PAUSE",
                "EXIT",
                "RESUME",
                "CLOSE"
            ],
            required: true
        },

        isRead: {
            type: Boolean,
            default: false
        },
        active: {
    type: Boolean,
    default: true
},





    },
    {
        timestamps: true
    }
);

module.exports = mongoose.model(
    "Notification",
    NotificationSchema
);
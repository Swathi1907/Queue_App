const mongoose = require("mongoose");

const notificationSchema = new mongoose.Schema(
    {
        hospitalId: {
            type: String,
            required: true,
            index: true
        },

        // Who should receive this notification
        targetRole: {
            type: String,
            enum: [
                "ADMIN",
                "DOCTOR",
                "COMPOUNDER",
                "PATIENT"
            ],
            required: true,
            index: true
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
                "DOCTOR_CHECKIN",
                "QUEUE_STARTED",
                "QUEUE_STATUS_UPDATE",
                "QUEUE_CLEARED",
                "SYSTEM_ALERT"
            ],
            required: true
        },

        department: {
            type: String,
            default: null
        },

        doctorCode: {
            type: String,
            default: null
        },

        isRead: {
            type: Boolean,
            default: false
        }
    },
    {
        timestamps: true
    }
);

module.exports = mongoose.model(
    "NewNotification",
    notificationSchema
);
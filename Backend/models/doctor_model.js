const mongoose = require("mongoose");

const doctorSchema = new mongoose.Schema({

    hospitalcode: {
        type: String,
        ref: "Hospital",
        required: true
    },

    doctorName: {
        type: String,
        required: true
    },

    specialization: {
        type: String,
        required: true
    },

    qualification: {
        type: String
    },

    roomNumber: {
        type: String
    },

    availableDays: [{
        type: String
    }],

    startTime: {
        type: String
    },

    endTime: {
        type: String
    }

}, {
    timestamps: true
});

module.exports = mongoose.model("Doctor", doctorSchema);
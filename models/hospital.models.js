const mongoose = require("mongoose");

const hospitalSchema = new mongoose.Schema({

    hospitalName: {
        type: String,
        required: true
    },

    hospitalId: {
        type: String,
        unique: true,
        required: true
    },

    address: {
        type: String,
        required: true
    },

    city: {
        type: String,
        required: true
    }

});

module.exports = mongoose.model("Hospital", hospitalSchema);
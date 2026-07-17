const mongoose = require('mongoose');

const queueMemberSchema = new mongoose.Schema({

    queueId: {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'Queue',
        required: true
    },
    queueName: {
        type: String,
        ref: 'Queue',
        required: true
    },

    avgServiceTime: {
        type: Number,
        ref: 'Queue'
    },

    userId: {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'User',
        required: true
    },

    tokenNumber: {
        type: Number,
        required: true
    },

    status: {
        type: String,
        enum: ['waiting', 'serving', 'completed', 'cancelled'],
        default: 'waiting'
    }
    ,
    readyTwoSent: {
        type: Boolean,
        default: false
    },

    readyOneSent: {
        type: Boolean,
        default: false
    },

    turnSent: {
        type: Boolean,
        default: false
    }
}, {
    timestamps: true
});

module.exports = mongoose.model('QueueMember', queueMemberSchema);
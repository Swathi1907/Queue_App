const mongoose = require('mongoose');

const queueSchema = new mongoose.Schema({

queueCapacity: {
    type: Number,
    required: true
},
latestToken: {
    type: Number,
    default: 0
},
    queueName: {
        type: String,
        required: true,
    //    unique: true
    },

    queueStatus: {
        type: String,
        enum: ['active', 'paused', 'closed'],
        default: 'active'
    },
totalPeople:{
 type: Number,
        default: 0
},
hospitalId: {
    type: String,
    required: true
},
roomNumber: {
    type: String,
    required: true
},

floor: {
    type: String,
    required: true
},

startTime: {
    type: String,
    required: true
},

endTime: {
    type: String,
    required: true
},

doctorName: {
    type: String,
    default: ""
},
    activeCount:{
  type: Number,
        default: 0
    },
    currentToken: {
        type: Number,
        default: null
    },

    avgServiceTime: {
        type: Number,
        default: 5
    },
    lastCompletedToken: {
    type: Number,
    default: 0
},
totalServiceTime: {
    type: Number,
    default: 0
},

completedPatients: {
    type: Number,
    default: 0
}
}, {
    timestamps: true
});

module.exports = mongoose.model('Queue', queueSchema);
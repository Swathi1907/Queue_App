const mongoose = require('mongoose');

const queueSchema = new mongoose.Schema({

    hospitalId: {
        type: String,
        required: true,
        index: true
    },

    department: {
        type: String,
        required: true,
        index: true
    },

    doctorCode: {
        type: String,
        required: true,
        index: true
    },

    date: {
        type: String,
        required: true
    },

    // Dynamic average service time in minutes
    // 5 is only the initial fallback
    avgServiceTime: {
        type: Number,
        default: 5
    },

    tokens: [
        {
            tokenNumber: {
                type: Number,
                required: true
            },
feedback: {
    status:          { type: String, enum: ['SUBMITTED', 'SKIPPED'] },
    doctorRating:    { type: Number, min: 1, max: 5 },
    doctorComment:   { type: String, maxlength: 500 },
    hospitalRating:  { type: Number, min: 1, max: 5 },
    hospitalComment: { type: String, maxlength: 500 },
    respondedAt:     { type: Date }
},
            userId: {
                type: mongoose.Schema.Types.ObjectId,
                ref: 'UserV2',
                required: true
            },

            patientName: {
                type: String,
                required: true
            },

            orderId: {
                type: String,
                required: true
            },

            paymentId: {
                type: String,
                required: true
            },

            amountPaid: {
                type: Number,
                required: true
            },

            status: {
                type: String,
                enum: [
                    'WAITING',
                    'IN_CONSULTATION',
                    'COMPLETED',
                    'CANCELLED'
                ],
                default: 'WAITING'
            },

            notes: {
                type: String,
                default: ""
            },

            // When patient received the token
            createdAt: {
                type: Date,
                default: Date.now
            },

            // When doctor actually started consultation
            consultationStartedAt: {
                type: Date,
                default: null
            },

            // When doctor completed consultation
            consultationCompletedAt: {
                type: Date,
                default: null
            },

            // Actual consultation duration in minutes
            serviceTime: {
                type: Number,
                default: null
            }
        }
    ],

    isActive: {
        type: Boolean,
        default: true
    },

    queueStatus: {
        type: String,
        enum: ['ACTIVE', 'PAUSED', 'CLOSED'],
        default: 'ACTIVE'
    }

}, {
    timestamps: true
});
queueSchema.index(
    {
        hospitalId: 1,
        doctorCode: 1
    },
    {
        unique: true
    }
);
module.exports =
    mongoose.models.new_queueV2 ||
    mongoose.model('new_queueV2', queueSchema);
const mongoose = require("mongoose");

const userActiveQueueSchema = new mongoose.Schema(
  {
    userId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "UserV2",
      required: true,
      index: true
    },

    queueId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "QueueV2",
      required: true,
      index: true
    },

    tokenId: {
      type: mongoose.Schema.Types.ObjectId,
      required: true
    },

    tokenNumber: {
      type: Number,
      required: true
    },

    // Hospital
    hospitalId: {
      type: String,
      required: true
    },

    hospitalName: {
      type: String,
      default: ""
    },

    hospitalLogoUrl: {
      type: String,
      default: ""
    },

    // Doctor
    doctorCode: {
      type: String,
      required: true
    },

    doctorName: {
      type: String,
      default: ""
    },

    // Queue details
    department: {
      type: String,
      default: ""
    },

    roomNumber: {
      type: String,
      default: ""
    },

    date: {
      type: String,
      default: ""
    },

    status: {
      type: String,
      enum: [
        "WAITING",
        "IN_CONSULTATION"
      ],
      default: "WAITING"
    },

    joinedAt: {
      type: Date,
      default: Date.now
    }
  },
  { timestamps: true }
);

userActiveQueueSchema.index({
  userId: 1,
  queueId: 1
});

module.exports = mongoose.model(
  "UserActiveQueue",
  userActiveQueueSchema
);
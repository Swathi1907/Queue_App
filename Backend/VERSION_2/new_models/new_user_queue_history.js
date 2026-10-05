const mongoose = require("mongoose");

const queueHistorySchema = new mongoose.Schema(
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
      required: true
    },

    tokenId: {
      type: mongoose.Schema.Types.ObjectId
    },

    tokenNumber: Number,

    hospitalId: String,

    hospitalName: String,

    hospitalLogoUrl: String,

    doctorCode: String,

    doctorName: String,

    department: String,

    roomNumber: String,

    status: {
      type: String,
      enum: [
        "COMPLETED",
        "CANCELLED",
        "LEFT"
      ]
    },

    date: String,

    feedback: {
      status: String,
      rating: Number,
      comment: String
    },

    joinedAt: Date,

    completedAt: Date
  },
  { timestamps: true }
);

queueHistorySchema.index({
  userId: 1,
  createdAt: -1
});

module.exports = mongoose.model(
  "QueueHistory",
  queueHistorySchema
);
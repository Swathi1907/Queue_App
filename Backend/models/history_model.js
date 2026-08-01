const  mongoose  = require("mongoose");
const history_schema= new mongoose.Schema({
  queueId: {
        type: mongoose.Schema.Types.ObjectId,
        ref: "Queue"
    },

    hospitalId: String,

    doctorName: String,

    department: String,

    tokenNumber: Number,

    peopleAhead: Number,

    serviceTime: Number,

    completedAt: Date

}, {
    timestamps: true
});

module.exports= mongoose.model("History",history_schema)


const Queue = require("../models/Queue.models");
const QueueMember = require("../models/queue_member.models");
const Hospital = require("../models/hospital.models");

async function buildQueueComparisions(userId,filters={}){
const queues = await Queue.find({
    queueStatus: { $in: ["active", "paused"] }
});
const result =[];
for(const queue of queues){
const hospital = await Hospital.findOne({
    hospitalId: queue.hospitalId
})
const waitingCount = await QueueMember.countDocuments({
queueId: queue._id,
status :{ $in : ['serving','waiting']}, 
})
result.push({
    hospitalName: hospital.hospitalName,
    department: queue.queueName,
    waitingPatients: waitingCount,
    avgServiceTime: queue.avgServiceTime,
    estimatedWait: waitingCount * queue.avgServiceTime,
    status: queue.queueStatus
});
}
return result;
}
module.exports = {
    buildQueueComparisions
}; 
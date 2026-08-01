const express = require('express');
const route = express.Router();
const Hospital = require('../models/hospital.models')
const Queue = require('../models/Queue.models');
const QueueMember = require('../models/queue_member.models');
const Authmiddleware = require('../middleware/authmiddleware');

route.get('/dashboard', Authmiddleware, async (req, res) => {
console.log("dashboard hit")

    try {
        const hospitalId = req.query.hospitalId;
      ;
        console.log("1");

const activeQueues = await Queue.countDocuments({
    hospitalId,
    queueStatus: "active"
});
console.log("2", activeQueues);

const hospital = await Hospital.findOne({
    hospitalId
});
console.log("3", hospital);

const queueIds = await Queue.find({ hospitalId }).select("_id");
console.log("4", queueIds);

const peopleWaiting = await QueueMember.countDocuments({
    queueId: { $in: queueIds.map(q => q._id) },
    status: "waiting"
});
console.log("5", peopleWaiting);

const startOfDay = new Date();
startOfDay.setHours(0, 0, 0, 0);

const servedToday = await QueueMember.countDocuments({
    queueId: { $in: queueIds.map(q => q._id) },
    status: "completed",
    updatedAt: { $gte: startOfDay }
});
console.log("6", servedToday);

const queues = await Queue.find({ hospitalId });
console.log("7", queues);

const avgWaitTime =
    queues.length > 0
        ? Math.round(queues.reduce((sum, q) => sum + (q.avgServiceTime || 0), 0) / queues.length)
        : 0;

console.log("8", avgWaitTime);

console.log("Sending response");

res.json({
   hospitalname: hospital.hospitalName,
    activeQueues,
    peopleWaiting,
    servedToday,
    avgWaitTime
});
/*const hospitalId = req.query.hospitalId;
        const activeQueues =
            await Queue.countDocuments({
          hospitalId,
                queueStatus: 'active' 
            });
const hospitalname= await Hospital.findOne({
      hospitalId: hospitalId,
    }).select("hospitalName")
    console.log(hospitalname)
       const queueIds = await Queue.find({
    hospitalId
}).select("_id");

const peopleWaiting =
    await QueueMember.countDocuments({
        queueId: {
            $in: queueIds.map(q => q._id)
        },
        status: "waiting"
    });

       const startOfDay = new Date();

startOfDay.setHours(
    0,
    0,
    0,
    0
);

const servedToday =
    await QueueMember.countDocuments({
        queueId: {
            $in: queueIds.map(q => q._id)
        },
        status: "completed",
        updatedAt: {
            $gte: startOfDay
        }
    });

       const queues = await Queue.find({
        hospitalId
       }
       );

const avgWaitTime =
    queues.length > 0
        ? queues.reduce(
              (sum, q) => sum + q.avgServiceTime,
              0
          ) / queues.length
        : 0;

        res.status(200).json({
            hospitalname,
            activeQueues,
            peopleWaiting,
            servedToday,
            avgWaitTime,
          
        });
*/
    } catch (err) {

        res.status(500).json({
            message: err.message
        });
    }
});

module.exports = route;
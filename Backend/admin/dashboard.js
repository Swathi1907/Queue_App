const express = require('express');
const route = express.Router();

const Queue = require('../models/Queue.models');
const QueueMember = require('../models/queue_member.models');
const Authmiddleware = require('../middleware/authmiddleware');

route.get('/dashboard', Authmiddleware, async (req, res) => {
console.log("dashboard hit")


    try {
const hospitalId = req.query.hospitalId;
        const activeQueues =
            await Queue.countDocuments({
                hospitalId,
                queueStatus: 'active'
            });

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
            activeQueues,
            peopleWaiting,
            servedToday,
            avgWaitTime
        });

    } catch (err) {

        res.status(500).json({
            message: err.message
        });
    }
});

module.exports = route;
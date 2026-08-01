const express = require('express');
const route = express.Router();
const QueueMember = require('../models/queue_member.models');
const Queue = require('../models/Queue.models');
const Authmiddleware = require('../middleware/authmiddleware');
const Notification = require("../models/notifications_model");
const sendNotification = require("../utils/sendNotification");
const User = require("../models/user.models");
const { getIO } = require("../socket");
const Hospital = require("../models/hospital.models");
const predicted = require('../services/predictTime')
const QueueHistory= require('../models/history_model')
const { calculateETA } = require("../services/etaService");
route.post('/:queueId/join', Authmiddleware, async (req, res) => {
    try {
        const queue = await Queue.findById(req.params.queueId);
        if (!queue) {
            return res.status(404).json({
                message: "Queue not found"
            });
        }


        if (queue.queueStatus === 'closed') {

            return res.status(400).json({
                message: "Queue is closed"
            });
        }
const hospitalQueues = await Queue.find({
    hospitalId: queue.hospitalId
}).select("_id");
const inQueue = await QueueMember.findOne({
    userId: req.user.userId,
    queueId: {
        $in: hospitalQueues.map(q => q._id)
    },
    status: { $in: ["waiting", "serving"] }
});

if (inQueue) {
    return res.status(400).json({
        message: "You are already in another queue of this hospital."
    });
}
    
        const curr_serving = await QueueMember.findOne({
            queueId: req.params.queueId,
            status: 'serving'
        });

        const lasttoken = await QueueMember.findOne({
            queueId: req.params.queueId
        }).sort({ tokenNumber: -1 });
        console.log(req.user);

        const nexttoken = lasttoken ? lasttoken.tokenNumber + 1 : 1;


        

        const newMember = new QueueMember({

            queueId: req.params.queueId,
            queueName: queue.queueName,
            userId: req.user.userId,
            tokenNumber: nexttoken,
            status: 'waiting',
            readyOneSent: false,

            readyTwoSent: false,

            turnSent: false
        });

        await newMember.save();
        console.log("curr_serving", curr_serving)
        console.log("next token:", nexttoken)
        console.log("Queue id: ", req.params.queueId)
        if (!curr_serving) {

            await Queue.findByIdAndUpdate(
                req.params.queueId,
                {
                    currentToken: nexttoken,
                    latestToken: nexttoken
                }
            );

        } else {

            await Queue.findByIdAndUpdate(
                req.params.queueId,
                {
                    latestToken: nexttoken
                }
            );
        }
        await Notification.create({
            userId: req.user.userId,
            queueId: req.params.queueId,
            title: "Joined Successfully",
            message: `You joined ${queue.queueName}. Your token number is ${nexttoken}.`,
            type: "JOIN"
        });
        const user = await User.findById(req.user.userId);

        if (user?.fcmToken) {
            await sendNotification(
                user.fcmToken,
                "Joined Successfully",
                `You joined ${queue.queueName}. Your token number is ${nexttoken}.`
            );
        }
        const io = getIO();
        io.emit("queueUpdated");
        const queueId = req.params.queueId
        console.log("MY STATUS QUEUE ID =", req.params.queueId)
        console.log("Queue updated successfully");
        res.status(200).json({
            message: "joined queue successfully",
            queueId: queueId,
            tokenNumber: nexttoken,
            name: req.user.name
        })
    }
    catch (err) {
        res.status(500).json({
            message: err.message,
        })
    }
})


route.post('/create', Authmiddleware, async (req, res) => {
    try {
        console.log("create hit");
        const existing = await Queue.findOne({
            queueName: req.body.queueName,
            hospitalId: req.body.hospitalId,
            queueStatus: { $in: ["active", "paused"] }
        });

        if (existing) {
            return res.status(400).json({
                message: "Queue already exists"
            });
        }
        console.log(req.body.startTime)
        const queue = new Queue({

            queueName: req.body.queueName,
            queueCapacity: req.body.queueCapacity,
            queueStatus: req.body.queueStatus,

            hospitalId: req.body.hospitalId,

            doctorName: req.body.doctorName,
            roomNumber: req.body.roomNumber,
            floor: req.body.floor,

            startTime: req.body.startTime,
            endTime: req.body.endTime

        });

        await queue.save();

        const io = getIO();
        io.emit("queueUpdated");

        res.status(201).json({
            message: "Queue created successfully",
            queue
        });

    } catch (err) {

        if (err.code === 11000) {
            return res.status(400).json({
                message: "Queue already exists"
            });
        }

        res.status(500).json({
            message: err.message
        });
    }
});



route.get('/allQueues', async (req, res) => {
    try {
        console.log("all queue hit")
        const { hospitalId } = req.query;
console.log("hospitalId =", hospitalId);
        if (!hospitalId) {
            return res.status(400).json({
                message: "Hospital ID is required"
            });
        }
        const allqueues = await Queue.find().select("hospitalId queueStatus queueName");

console.log(allqueues);
        const queues = await Queue.find({
            hospitalId: req.query.hospitalId,
            queueStatus: { $in: ['active', 'paused'] }
        });
console.log("Queues found =", queues.length);
console.log(queues);


        const result = await Promise.all(
            queues.map(async (queue) => {

                const lastActiveMember = await QueueMember.findOne({
                    queueId: queue._id,
                    status: { $in: ['waiting', 'serving'] }
                }).sort({ tokenNumber: -1 });

                const currentMember = await QueueMember.findOne({
                    queueId: queue._id,
                    status: "serving"
                });
                const totalPeople = await QueueMember.countDocuments({
                    queueId: queue._id,
                    status: { $in: ['completed', 'waiting', 'serving'] }
                });


                //const allMembers = await QueueMember.find();

                //console.log(
                //  allMembers.map(m => ({
                //    queueId: m.queueId.toString(),
                //  status: m.status,
                //token: m.tokenNumber
                // }))
                //);
                const waiting_members = await QueueMember.countDocuments({
                    queueId: queue._id,
                    status: { $in: ["waiting"] }
                });
                const activeCount = await QueueMember.countDocuments({
                    queueId: queue._id,
                    status: { $in: ["waiting", "serving"] }
                });

                console.log("Active Count:", activeCount);
                const total = await QueueMember.countDocuments({
                    queueId: queue._id
                });

                console.log("Total =", total);
                console.log(activeCount);
                return {
                    ...queue.toObject(),
                    totalPeople,
                    activeCount: activeCount,
                    currentToken: currentMember
                        ? currentMember.tokenNumber
                        : null,

                    latestToken: lastActiveMember
                        ? lastActiveMember.tokenNumber
                        : 0,
                    waiting_members
                };
            })
        );

        res.status(200).json(result);

    } catch (err) {
        res.status(500).json({
            message: err.message
        });
    }
});
route.get("/:queueId/myStatus", Authmiddleware, async (req, res) => {
    try {

        const user = await QueueMember.findOne({
            queueId: req.params.queueId,
            userId: req.user.userId,
            status: { $in: ["waiting", "serving"] }
        });

        if (!user) {
            return res.status(404).json({
                message: "You are not in this queue"
            });
        }

        const queue = await Queue.findById(req.params.queueId);

        if (!queue) {
            return res.status(404).json({
                message: "Queue not found"
            });
        }

        const etaData = await calculateETA(queue, user);

        res.status(200).json({

            esttime: etaData.eta,

            queueName: queue.queueName,

            yourToken: user.tokenNumber,

            peopleAhead: etaData.peopleAhead,

            activeCount: etaData.activeCount,

            currentToken: etaData.currentMember
                ? etaData.currentMember.tokenNumber
                : null,

            totalPeople: etaData.totalPeople,

            latestToken: etaData.lastActiveMember
                ? etaData.lastActiveMember.tokenNumber
                : 0,

            lastCompletedToken: queue.lastCompletedToken,

            status: user.status,

            avgServiceTime: Math.round(etaData.avgServiceTime),

            userId: req.user.userId,

            progress: etaData.progress,

            queueStarted: etaData.servingMember != null,

            queue_status: etaData.queue_status,

            QueueStatus: queue.queueStatus
        });

    } catch (err) {

        res.status(500).json({
            message: err.message
        });
    }
});




route.get('/:queueId/allMembers', Authmiddleware, async (req, res) => {
    try {

        const queueMembers = await QueueMember.find({

            queueId: req.params.queueId
        }).populate("userId", "name")
            .sort({ tokenNumber: 1 });

        const result = queueMembers.map(member => ({

            name: member.userId.name,
            tokenNumber: member.tokenNumber,
            status: member.status,
            isMe: member.userId.toString() === req.user.userId,

        }));

        res.status(200).json(result);

    } catch (err) {
        res.status(500).json({
            message: err.message
        });
    }
});

route.get('/:queueId/userMembers', Authmiddleware, async (req, res) => {
    try {

        console.log("members api hit");
        // Find the logged-in user in this queue
        const me = await QueueMember.findOne({
            queueId: req.params.queueId,
            userId: req.user.userId,
            status: { $in: ["waiting", "serving"] }
        });

        if (!me) {
            return res.status(404).json({
                message: "You are not in this queue"
            });
        }

        // Check if someone is currently being served
        const serving = await QueueMember.findOne({
            queueId: req.params.queueId,
            status: "serving"
        });

        let startToken;

        if (serving) {
            startToken = serving.tokenNumber;
        } else {
            // No one is serving yet, start from the first waiting member
            const firstWaiting = await QueueMember.findOne({
                queueId: req.params.queueId,
                status: "waiting"
            }).sort({ tokenNumber: 1 });

            if (!firstWaiting) {

                return res.status(200).json([]);
            }

            startToken = firstWaiting.tokenNumber;
        }

        const members = await QueueMember.find({
            queueId: req.params.queueId,
            tokenNumber: {
                $gte: startToken,
                $lte: me.tokenNumber
            },
            status: { $in: ["cancelled", "waiting", "serving"] }
        }).populate("userId", "name")
            .sort({ tokenNumber: 1 });
        console.log("MEMBERS AFTER POPULATE");
        console.log(JSON.stringify(members, null, 2));
        const result = members.map(member => ({
            name: member.userId.name,
            tokenNumber: member.tokenNumber,
            status: member.status,
            isMe: member.userId._id.toString() === req.user.userId
        }));
        console.log(result.name)
        res.status(200).json(result);

    } catch (err) {
        res.status(500).json({
            message: err.message
        });
    }
});

route.get("/admin/activequeues", Authmiddleware, async (req, res) => {
    try {

        console.log("ACTIVE API HIT");

        const { hospitalId } = req.query;

        if (!hospitalId) {
            return res.status(400).json({
                message: "Hospital ID is required"
            });
        }

        const queues = await Queue.find({
            hospitalId: hospitalId,
            queueStatus: { $in: ["active", "paused"] }
        });

        const result = await Promise.all(
            queues.map(async (queue) => {

                const waitingCount = await QueueMember.countDocuments({
                    queueId: queue._id,
                    status: "waiting"
                });

                const servingMember = await QueueMember.findOne({
                    queueId: queue._id,
                    status: "serving"
                });

                return {
                    queueId: queue._id,
                    queueName: queue.queueName,
                    status: queue.queueStatus,
                    waitingCount,
                    servingToken: servingMember
                        ? servingMember.tokenNumber
                        : null
                };
            })
        );

        res.json(result);

    } catch (err) {
        res.status(500).json({
            message: err.message
        });
    }
});
route.post('/:queueId/completeCurrent', Authmiddleware, async (req, res) => {

    try {

        const queue = await Queue.findById(
            req.params.queueId
        );

        if (!queue) {

            return res.status(404).json({
                message: "Queue not found"
            });
        }

        const currentMember =
            await QueueMember.findOne({

                queueId: req.params.queueId,
                status: 'serving'

            });

        if (!currentMember) {

            return res.status(404).json({
                message: "No patient is currently being served"
            });
        }

        currentMember.status = 'completed';
currentMember.completedAt = new Date();
const duration =
    (currentMember.completedAt - currentMember.servingStartedAt) / (1000 * 60);
        await currentMember.save();
const peopleAhead = await QueueMember.countDocuments({
    queueId: req.params.queueId,
    tokenNumber: { $lt: currentMember.tokenNumber },
    status: { $in: ["waiting", "serving"] }
});
await QueueHistory.create({

    queueId: queue._id,

    hospitalId: queue.hospitalId,

    doctorName: queue.doctorName,

    department: queue.queueName,

    tokenNumber: currentMember.tokenNumber,

    peopleAhead,

    serviceTime: Math.round(duration),

    completedAt: currentMember.completedAt

});
await Notification.deleteMany({
    userId: currentMember.userId,
    queueId: currentMember.queueId
});

console.log("Before:", {
    total: queue.totalServiceTime,
    completed: queue.completedPatients
});

queue.totalServiceTime += duration;
queue.completedPatients += 1;
queue.avgServiceTime = Math.ceil(
    queue.totalServiceTime / queue.completedPatients
);
console.log("After:", {
    total: queue.totalServiceTime,
    completed: queue.completedPatients
});
        queue.lastCompletedToken = currentMember.tokenNumber;


        await queue.save();
        const waitingMembers = await QueueMember.find({
            queueId: req.params.queueId,
            status: "waiting"
        }).sort({ tokenNumber: 1 });

        if (waitingMembers.length >= 1) {

            const member = waitingMembers[0];

            if (!member.readyOneSent) {
                console.log("BEFORE CREATE");
                await Notification.create({
                    userId: member.userId,
                    queueId: req.params.queueId,
                    title: "Almost Your Turn!",
                    message: "You are next. Please be ready.",
                    type: "READY_1"
                });
                console.log("After CREATE");
                const user = await User.findById(member.userId);
                console.log("FCM Token:", user?.fcmToken);
                if (user?.fcmToken) {
                    await sendNotification(
                        user.fcmToken,
                        "Almost Your Turn!",
                        "You are next. Please be ready."
                    );
                }

                member.readyOneSent = true;
                await member.save();
            }
        }


        if (waitingMembers.length >= 2) {

            const member = waitingMembers[1];

            if (!member.readyTwoSent) {

                await Notification.create({
                    userId: member.userId,
                    queueId: req.params.queueId,
                    title: "Get Ready!",
                    message: "Only 1 person is ahead of you.",
                    type: "READY_2"
                });

                const user = await User.findById(member.userId);

                if (user?.fcmToken) {
                    await sendNotification(
                        user.fcmToken,
                        "Get Ready!",
                        "Only 1 person is ahead of you."
                    );
                }

                member.readyTwoSent = true;
                await member.save();
            }
        }
        queue.currentToken = null;

        await queue.save();
console.log("Started:", currentMember.servingStartedAt);
console.log("Completed:", currentMember.completedAt);
console.log("Duration:", duration);
        console.log("Emitting queueUpdated");
        const io = getIO();
        io.emit("queueUpdated");
        console.log("queueUpdated emitted");
        return res.status(200).json({

            message: "Current patient completed successfully"

        });

    } catch (err) {

        return res.status(500).json({

            message: err.message

        });
    }
});

route.delete("/reset", async (req, res) => {
    try {

        await Queue.deleteMany({});
        await QueueMember.deleteMany({});
        await Notification.deleteMany({});

        res.json({
            message: "All queue data deleted successfully"
        });

    } catch (err) {
        res.status(500).json({
            message: err.message
        });
    }
});
route.post('/:queueId/next', Authmiddleware, async (req, res) => {

    try {
        console.log("next called")
        const queue = await Queue.findById(req.params.queueId);
        const curr_serving = await QueueMember.findOne({

            queueId: req.params.queueId,
            status: 'serving'

        }).sort({ tokenNumber: 1 });
        if (curr_serving) {
            return res.status(200).json({
                message: "complete the current member",
            })
        }
        const nextToken = await QueueMember.findOne({

            queueId: req.params.queueId,
            status: 'waiting'

        }).sort({ tokenNumber: 1 });


        console.log("NEXT TOKEN =", nextToken);
        if (!nextToken) {

            return res.status(200).json({
                message: "No waiting members"
            });
        }

        nextToken.status = "serving";
nextToken.servingStartedAt = new Date();
        console.log("AFTER SAVE");
        console.log("turnSent =", nextToken.turnSent);
        await nextToken.save();

        queue.currentToken = nextToken.tokenNumber;

        await queue.save();
        // It's Your Turn

        console.log("turnSent =", nextToken.turnSent);
        if (!nextToken.turnSent) {

            console.log("ENTERED TURN BLOCK");

            await Notification.create({

                userId: nextToken.userId,

                queueId: req.params.queueId,

                title: "It's Your Turn!",

                message: `Please proceed to ${queue.queueName}.`,

                type: "TURN"

            });

            console.log("NOTIFICATION CREATED");

            nextToken.turnSent = true;
            await nextToken.save();

            console.log("TURN FLAG UPDATED");

            const user = await User.findById(nextToken.userId);

            console.log("USER =", user);
            console.log("FCM Token:", user?.fcmToken);
            if (user?.fcmToken) {

                console.log("SENDING FCM");

                await sendNotification(
                    user.fcmToken,
                    "It's Your Turn!",
                    `Please proceed to ${queue.queueName}.`
                );

                console.log("FCM SENT");
            }
        }

        console.log("Emitting queueUpdated");
        const io = getIO();
        io.emit("queueUpdated");
        console.log("queueUpdated emitted");
        return res.status(200).json({
            message: "Next patient called successfully"
        });

    } catch (err) {

        return res.status(500).json({
            message: err.message
        });
    }
});


route.post('/:queueId/exit', Authmiddleware, async (req, res) => {

    try {
        console.log("exit hit");
        const member = await QueueMember.findOne({
            queueId: req.params.queueId,
            userId: req.user.userId,
            status: { $in: ["waiting", "serving"] }
        }).sort({ createdAt: -1 });

        if (!member) {
            return res.status(404).json({
                message: "You are not in this queue"
            });
        }

        if (member.status === "serving") {
            return res.status(400).json({
                message: "You cannot leave while being served"
            });
        }

        if (
            member.status === "completed" ||
            member.status === "cancelled"
        ) {
            return res.status(400).json({
                message: "You have already left this queue"
            });
        }

        member.status = "cancelled";
        await member.save();
        
   
const queue = await Queue.findById(req.params.queueId);

await Notification.deleteMany({
    userId: req.user.userId,
    queueId: req.params.queueId
});
console.log("Creating exit notification...");
try {
    const notification = await Notification.create({
    userId: req.user.userId,
    queueId: req.params.queueId,
    title: "Queue Exited",
    message: `You have successfully exited the ${queue.queueName} queue.`,
    type: "EXIT",
    isRead: false
});

    console.log("Notification created:", notification);

} catch (err) {
    console.log("Notification error:", err);
}
        console.log("exit notification created...");
        const user = await User.findById(req.user.userId);
  if (user?.fcmToken) {
    console.log("FCM SENDING");
            await sendNotification(
                user.fcmToken,
                "Exited Successfully",
                `You exited ${queue.queueName}.`
            );
        }
        const lastActiveMember = await QueueMember.findOne({
            queueId: req.params.queueId,
            status: { $in: ["waiting", "serving"] }
        }).sort({ tokenNumber: -1 });

        queue.latestToken = lastActiveMember
            ? lastActiveMember.tokenNumber
            : queue.lastCompletedToken;

        await queue.save();
        console.log("EXIT: emitting queueUpdated");
        const io = getIO();
        io.emit("queueUpdated");
        console.log("EXIT: emitted");
        return res.status(200).json({
            message: "Exited from queue successfully"
        });

    } catch (err) {

        return res.status(500).json({
            message: err.message
        });
    }
});
route.get('/myActiveQueue', Authmiddleware, async (req, res) => {

    try {
console.log("myactiveapi hit")
      const members = await QueueMember.find({
    userId: req.user.userId,
    status: { $in: ['waiting', 'serving'] }
}).populate("queueId");
        if (members.length === 0) {

            return res.status(200).json([]);
        }
const result = [];
for (const member of members) {

    const queue = member.queueId;

    const hospital = await Hospital.findOne({
        hospitalId: queue.hospitalId
    });

    const etaData = await calculateETA(queue, member);

  result.push({
    hospitalName: hospital.hospitalName,
    hospitalId: queue.hospitalId,

    branchName: hospital.city,

    queueId: queue._id,
    queueName: queue.queueName,

    yourToken: member.tokenNumber,

    peopleAhead: etaData.peopleAhead,
    activeCount: etaData.activeCount,

    currentToken: etaData.currentMember
        ? etaData.currentMember.tokenNumber
        : null,

    totalPeople: etaData.totalPeople,

    latestToken: etaData.lastActiveMember
        ? etaData.lastActiveMember.tokenNumber
        : 0,

    lastCompletedToken: queue.lastCompletedToken,

    status: member.status,

    avgServiceTime: Math.round(etaData.avgServiceTime),

    esttime: etaData.eta,

    progress: etaData.progress,

    queueStarted: etaData.servingMember != null,

    queue_status: etaData.queue_status,

    QueueStatus: queue.queueStatus
});
}
       console.log("FINAL RESULT =", JSON.stringify(result, null, 2));
        res.status(200).json(result);

    } catch (err) {

        res.status(500).json({
            message: err.message
        });
    }
});

route.post('/:queueId/toggleQueueStatus', Authmiddleware, async (req, res) => {

    try {

        const queue = await Queue.findById(
            req.params.queueId
        );

        if (!queue) {
            return res.status(404).json({
                message: "Queue not found"
            });
        }

        if (queue.queueStatus === 'active') {

            queue.queueStatus = 'paused';

        } else if (queue.queueStatus === 'paused') {

            queue.queueStatus = 'active';
        }

        await queue.save();

        const members = await QueueMember.find({
            queueId: req.params.queueId,
            status: { $in: ["waiting", "serving"] }
        });

        for (const member of members) {

            const user = await User.findById(member.userId);

            if (!user?.fcmToken) continue;

            await sendNotification(
                user.fcmToken,
                queue.queueStatus === "paused"
                    ? "Queue Paused"
                    : "Queue Resumed",
                queue.queueStatus === "paused"
                    ? `${queue.queueName} has been paused.`
                    : `${queue.queueName} has resumed.`
            );
        }
        const io = getIO();
        io.emit("queueUpdated");
        return res.status(200).json({
            message: `Queue ${queue.queueName} ${queue.queueStatus}`,
            queueStatus: queue.queueStatus
        });

    } catch (err) {

        return res.status(500).json({
            message: err.message
        });
    }
});
route.post('/:queueId/close', Authmiddleware, async (req, res) => {

    try {

        const queue = await Queue.findById(
            req.params.queueId
        );

        if (!queue) {

            return res.status(404).json({
                message: "Queue not found"
            });
        }

        queue.queueStatus = 'closed';

        await queue.save();
        await QueueMember.updateMany(
            {
                queueId: req.params.queueId,
                status: {
                    $in: ['waiting', 'serving']
                }
            },
            {
                status: 'completed'
            }
        );
        await Notification.deleteMany({
            queueId: req.params.queueId
        });
        const io = getIO();
        io.emit("queueUpdated");
        return res.status(200).json({
            message: "Queue closed successfully"
        });

    } catch (err) {

        return res.status(500).json({
            message: err.message
        });
    }
});
route.get('/:queueId/details', Authmiddleware, async (req, res) => {

    try {

        const queue = await Queue.findById(req.params.queueId);

        if (!queue) {
            return res.status(404).json({
                message: "Queue not found"
            });
        }

        const waitingUsers = await QueueMember.countDocuments({
            queueId: req.params.queueId,
            status: 'waiting'
        });

        const lastActiveMember = await QueueMember.findOne({
            queueId: req.params.queueId,
            status: { $in: ['waiting', 'serving'] }
        }).sort({ tokenNumber: -1 });

        const latestToken = lastActiveMember
            ? lastActiveMember.tokenNumber
            : 0;

        const currentMember = await QueueMember.findOne({
            queueId: req.params.queueId,
            status: 'serving'
        });

        const currentToken = currentMember
            ? currentMember.tokenNumber
            : 0;

        res.status(200).json({
            queueName: queue.queueName,
            avgServiceTime: queue.avgServiceTime,
            currentToken,
            latestToken,
            queueStatus: queue.queueStatus,
            waitingUsers
        });

    } catch (err) {

        res.status(500).json({
            message: err.message
        });
    }
});
route.get('/:queueId/predicttime',Authmiddleware,async(req,res)=>{
    const queue = await Queue.findById(req.params.queueId);
    if (!queue) {
    return res.status(404).json({
        message: "Queue not found"
    });
}
     const user = await QueueMember.findOne({
            queueId: req.params.queueId,
            userId: req.user.userId,
            status: { $in: ['waiting', 'serving'] }
        });

        if (!user) {
            return res.status(404).json({
                message: "You are not in this queue"
            });
        }
     const peopleAhead = await QueueMember.countDocuments({
            queueId: req.params.queueId,
            tokenNumber: { $lt: user.tokenNumber },
            status: { $in: ["waiting", "serving"] }
        });
const predictedServiceTime = await predicted(req.params.queueId);
const eta =
peopleAhead * predictedServiceTime;
res.json({
    peopleAhead,
    predictedServiceTime,
    estimatedWaitTime: eta
});
})
module.exports = route;
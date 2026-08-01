const QueueMember = require("../models/queue_member.models");

async function calculateETA(queue, member) {

    const currentMember = await QueueMember.findOne({
        queueId: queue._id,
        status: "serving"
    });

    const servingMember = currentMember;

    const activeCount = await QueueMember.countDocuments({
        queueId: queue._id,
        status: { $in: ["waiting", "serving"] }
    });

    const totalPeople = await QueueMember.countDocuments({
        queueId: queue._id,
        status: { $in: ["completed", "waiting", "serving"] }
    });

    const peopleAhead = await QueueMember.countDocuments({
        queueId: queue._id,
        tokenNumber: { $lt: member.tokenNumber },
        status: { $in: ["waiting", "serving"] }
    });

    const waitingAhead = await QueueMember.countDocuments({
        queueId: queue._id,
        tokenNumber: { $lt: member.tokenNumber },
        status: "waiting"
    });

    const lastActiveMember = await QueueMember.findOne({
        queueId: queue._id,
        status: { $in: ["waiting", "serving"] }
    }).sort({ tokenNumber: -1 });

    const avgServiceTime = queue.avgServiceTime || 5;

   let remaining = 0;

if (servingMember?.servingStartedAt) {

    const elapsed =
        (Date.now() - servingMember.servingStartedAt.getTime()) / (1000 * 60);

    if (elapsed < avgServiceTime) {
        remaining = avgServiceTime - elapsed;
    } else {
        // Patient has exceeded the average
        remaining = 1;
    }
}

    let queue_status;

    if (member.status === "serving") {

        queue_status = "SERVING";

    } else if (!servingMember) {

        if (queue.lastCompletedToken === 0) {

            queue_status = "WAITING_TO_START";

        } else if (peopleAhead === 0) {

            queue_status = "WAITING_FOR_NEXT_CALL";

        } else {

            queue_status = "WAITING";
        }

    } else {

        if (peopleAhead === 0) {

            queue_status = "NEXT";

        } else {

            queue_status = "WAITING";
        }
    }

    let eta;

    if (member.status === "serving") {

        eta = 0;

    } else if (!servingMember) {

        if (queue.lastCompletedToken === 0) {

            eta = -1;

        } else if (waitingAhead === 0) {

            eta = 0;

        } else {

            eta = waitingAhead * avgServiceTime;
        }

    } else {

        eta = remaining + waitingAhead * avgServiceTime;
    }

    if (eta != null) {
        eta = Math.round(eta);
    }

    const completedCount = await QueueMember.countDocuments({
        queueId: queue._id,
        status: "completed"
    });

    
let progress = completedCount + (servingMember ? 1 : 0);

if (member.status === "serving") {
    progress = totalPeople;
}
    return {
        eta,
        queue_status,
        avgServiceTime,
        activeCount,
        totalPeople,
        peopleAhead,
        waitingAhead,
        remaining,
        progress,
        currentMember,
        servingMember,
        lastActiveMember
    };
}

module.exports = {
    calculateETA
};
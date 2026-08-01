const QueueMember = require("../models/queue_member.models");

async function buildQueueContext(userId) {

    const members = await QueueMember.find({
        userId,
        status: { $in: ["waiting", "serving"] }
    }).populate("queueId");

    if (members.length === 0) {
        return null;
    }

    let queueContext = "";

    for (const member of members) {

        const queue = member.queueId;

        const servingMember = await QueueMember.findOne({
            queueId: queue._id,
            status: "serving"
        });

        const peopleAhead = await QueueMember.countDocuments({
            queueId: queue._id,
            tokenNumber: { $lt: member.tokenNumber },
            status: { $in: ["waiting", "serving"] }
        });

        const previousTokens = await QueueMember.find({
            queueId: queue._id,
            tokenNumber: { $lt: member.tokenNumber }
        }).sort({ tokenNumber: 1 });

        const previousTokenInfo =
            previousTokens.length > 0
                ? previousTokens
                      .map(t => `Token ${t.tokenNumber} - ${t.status}`)
                      .join("\n")
                : "None";

        queueContext += `
--------------------------------

Department: ${queue.queueName}

Queue Status: ${queue.queueStatus}

Your Token: ${member.tokenNumber}

Current Token: ${
    servingMember
        ? servingMember.tokenNumber
        : "Queue has not started"
}

People Ahead: ${peopleAhead}

Tokens Before You:
${previousTokenInfo}

Start Time: ${queue.startTime}

End Time: ${queue.endTime}

`;
    }

    return queueContext;
}

module.exports = {
    buildQueueContext
};
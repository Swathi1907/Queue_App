const QueueV2 = require('../new_models/new_queuev2');
const UserV2 = require('../new_models/peron_model');
const HospitalV2 = require('../new_models/new_hosp_model');
const mongoose = require('mongoose');
const oid = (v) => new mongoose.Types.ObjectId(v);
const {
    sendNotification
} = require("../new_controllers/notification_controller");

const createDepartmentQueue = async (req, res) => {
  try {
    console.log("hit");
    const { hospitalId, department, doctorCode, queueStatus } = req.body;

    // 1. Verify the doctor exists and is assigned to this department and hospital
    const doctor = await UserV2.findOne({ doctorCode, hospitalId, role: 'DOCTOR' });
    if (!doctor) {
      return res.status(404).json({ success: false, message: 'Doctor not found in this hospital.' });
    }
    
    if (!doctor.department.includes(department)) {
      return res.status(400).json({
        success: false,
        message: `Doctor belongs to the ${doctor.department.join(', ')} department, not ${department}.`
      });
    }
const today = new Date().toISOString().split('T')[0];
    // 2. Optional safety check: Prevent creating a new queue if an ACTIVE or PAUSED one already exists for this doctor/department
    const existingActiveQueue = await QueueV2.findOne({
      doctorCode,
      department,
      queueStatus: { $ne: 'CLOSED' }
    });

    if (existingActiveQueue) {
      return res.status(400).json({
        success: false,
        message: 'An active or paused queue already exists for this department. Please close it before starting a new one.',
        data: existingActiveQueue
      });
    }

    // 3. Create a brand new independent queue document every time
    const newQueue = await QueueV2.create({
      hospitalId,
      department,
      doctorCode,
      date: today,
      queueStatus: queueStatus || 'ACTIVE',
      tokens: []
    });

    await sendNotification({
      hospitalId: hospitalId,
      targetRole: "ADMIN",
      title: "Queue Started",
      message: `${department} queue has been started by Dr. ${doctor.name}.`,
      type: "QUEUE_STARTED",
      department: department,
      doctorCode: doctorCode
    });

    console.log("created", newQueue);
    return res.status(201).json({
      success: true,
      message: 'Department queue created successfully.',
      data: newQueue
    });

  } catch (error) {
    console.error("Error in createDepartmentQueue:", error);
    return res.status(500).json({ success: false, error: error.message });
  }
};
const getLiveQueueTicket = async (req, res) => {
    try {
        const { queueId } = req.query;
const userId=req.params.userId
console.log(userId)
        if (!queueId || !userId) {
            return res.status(400).json({
                success: false,
                message: 'queueId and userId are required query parameters.'
            });
        }

        // 1. Fetch the queue document
        const queue = await QueueV2.findById(queueId).lean();
        if (!queue) {
            return res.status(404).json({ success: false, message: 'Queue session not found.' });
        }

        // 2. Find the specific user's token in this queue
        const userToken = queue.tokens.find(t => t.userId.toString() === userId);
        if (!userToken) {
            return res.status(404).json({ success: false, message: 'Token not found for this user in the queue.' });
        }

        // 3. Fetch hospital details
        const hospital = await HospitalV2.findOne({ 
            $or: [{ code: queue.hospitalId }, { _id: queue.hospitalId.match(/^[0-9a-fA-F]{24}$/) ? queue.hospitalId : null }] 
        }).lean();

        // 4. Fetch doctor details
        const doctor = await UserV2.findOne({ 
            $or: [
                { doctorCode: queue.doctorCode }, 
                { code: queue.doctorCode }, 
                { _id: queue.doctorCode && queue.doctorCode.match(/^[0-9a-fA-F]{24}$/) ? queue.doctorCode : null }
            ],
            role: "DOCTOR"
        }).lean();

        const rawName = doctor?.name ? doctor.name.replace(/^dr\.?\s*/i, '') : queue.doctorCode;
        const doctorDisplayName = rawName ? `Dr. ${rawName}` : (queue.doctorCode || "Doctor");

        // 5. Calculate people ahead and estimated wait time
        const waitingTokens = queue.tokens.filter(t => t.status === "WAITING");
        const userIndex = waitingTokens.findIndex(t => t.userId.toString() === userId && t.tokenNumber === userToken.tokenNumber);
        const peopleAhead = userIndex > 0 ? userIndex : 0;
        
        // Use queue's avgServiceTime (default to 5 mins if not set)
        const avgServiceTime = queue.avgServiceTime || 5;
        const estWaitTime = peopleAhead * avgServiceTime;

        // Determine dynamic message
        let queueMessage = "Please wait for your turn.";
        if (peopleAhead === 0 && userToken.status === "WAITING") {
            queueMessage = "You are next! Please proceed near Room 04";
        } else if (userToken.status === "IN_CONSULTATION") {
            queueMessage = "You are currently in consultation.";
        } else {
            queueMessage = `${peopleAhead} people ahead of you. Estimated wait: ${estWaitTime} min`;
        }

        // 6. Response payload matching your layout components
        return res.status(200).json({
            success: true,
            data: {
                hospitalName: hospital ? hospital.name : "APOLLO HOSPITAL",
                hospitalLogoUrl: hospital ? hospital.logoUrl : "",
                doctorName: doctorDisplayName,
                department: queue.department || "Emergency Department",
                roomNumber: doctor?.roomNumber || "Room 04",
                isDoctorOnDuty: queue.queueStatus === 'ACTIVE',
                tokenNumber: userToken.tokenNumber,
                status: userToken.status, // WAITING, IN_CONSULTATION, COMPLETED, CANCELLED
                queueMessage: queueMessage,
                peopleAheadText: `${peopleAhead} people`,
                estWaitTimeText: `${estWaitTime} min`,
                queueDate: queue.date,
                isPaid: userToken.amountPaid > 0,
                paymentText: userToken.amountPaid > 0 ? "✓ Paid" : "Pending"
            }
        });

    } catch (error) {
        console.error("Error fetching live queue ticket:", error);
        return res.status(500).json({ success: false, error: error.message });
    }
};
const getUserQueuesDashboard = async (req, res) => {
  try {
    console.log("user queues dash hit")
    const { userId } = req.params;
    console.log(userId)
    const today = new Date().toISOString().split('T')[0];

    // 1. Fetch ALL queues where the user has at least one token
    const allUserQueues = await QueueV2.find({
      "tokens.userId": userId
    })
    .sort({ createdAt: -1 })
    .lean();

    const activeQueueList = [];
    const historyList = [];

    // 2. Loop through all queues
    for (const queue of allUserQueues) {
      // Find ALL tokens belonging to this user in the queue (supports multiple joins)
      const userTokens = queue.tokens.filter(t => t.userId.toString() === userId);
      if (userTokens.length === 0) continue;

      // Fetch hospital details
      const hospital = await HospitalV2.findOne({ 
        $or: [{ code: queue.hospitalId }, { _id: queue.hospitalId.match(/^[0-9a-fA-F]{24}$/) ? queue.hospitalId : null }] 
      }).lean();

      // Fetch doctor details
      const doctor = await UserV2.findOne({ 
        $or: [
          { doctorCode: queue.doctorCode }, 
          { code: queue.doctorCode }, 
          { _id: queue.doctorCode && queue.doctorCode.match(/^[0-9a-fA-F]{24}$/) ? queue.doctorCode : null }
        ],
        role: { $in: ["DOCTOR", "COMPOUNDER"] }
      }).lean();

      const rawName = doctor?.name ? doctor.name.replace(/^dr\.?\s*/i, '') : queue.doctorCode;
      const doctorDisplayName = rawName ? `Dr. ${rawName}` : (queue.doctorCode || "Doctor");

      // Evaluate each token session separately
      for (const userToken of userTokens) {
      const isQueueActive =
  queue.queueStatus === "ACTIVE" ||
  queue.queueStatus === "PAUSED";
const isActiveToday =
  isQueueActive &&
  userToken.status !== "COMPLETED" &&
  userToken.status !== "CANCELLED";
        const queueItemData = {
          queueId: queue._id,
          hospitalName: hospital ? hospital.name : "City Central Hospital",
          tokenId: userToken._id,                                // add
  feedbackStatus: userToken.feedback?.status || null, 
          hospitalLogoUrl: hospital ? hospital.logoUrl : "",
          doctorDetails: `${queue.department} •${doctorDisplayName}`,
          status: userToken.status, 
          tokenNumber: userToken.tokenNumber,
          date: queue.date,
          createdAt: queue.createdAt
        };

        if (isActiveToday) {
          const waitingTokens = queue.tokens.filter(t => t.status === "WAITING");
          const userIndex = waitingTokens.findIndex(t => t.userId.toString() === userId && t.tokenNumber === userToken.tokenNumber);
          const peopleAhead = userIndex > 0 ? userIndex : 0;
          const avgServiceTime = queue.avgServiceTime || 5;

const estWaitTime = Math.max(1,Math.round(
  peopleAhead * avgServiceTime
));

          activeQueueList.push({
            ...queueItemData,
            peopleAheadText: `${peopleAhead} people ahead`,
            estWaitTimeText: `${estWaitTime} min`
          });
        } else {
          historyList.push({
            ...queueItemData,
            subText: `${queue.department} • ${doctorDisplayName} (${queue.date})`
          });
        }
      }
    }

    // 3. Sort both lists descending by creation/date
    activeQueueList.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
    historyList.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));

    return res.status(200).json({
      success: true,
      data: {
        activeQueue: activeQueueList,
        recentHistory: historyList.slice(0, 5)
      }
    });

  } catch (error) {
    console.log(error.message)
    return res.status(500).json({ success: false, error: error.message });
  }
};
const leaveQueue = async (req, res) => {
  try {
    const { queueId } = req.body;
    const userId  = req.user.id;  // Or req.user.id if you use auth middleware

    if (!queueId || !userId) {
      return res.status(400).json({
        success: false,
        message: 'queueId in parameters and userId in body are required.'
      });
    }

    // 1. Find the queue document
    const queue = await QueueV2.findById(queueId);
    if (!queue) {
      return res.status(404).json({
        success: false,
        message: 'Queue session not found.'
      });
    }

    // 2. Find the user's token index (must be in WAITING state to leave)
    const tokenIndex = queue.tokens.findIndex(
      t => t.userId.toString() === userId.toString() && t.status === 'WAITING'
    );

    if (tokenIndex === -1) {
      // Check if they have any token at all to give a more accurate error message
      const existingToken = queue.tokens.find(t => t.userId.toString() === userId.toString());
      
      if (!existingToken) {
        return res.status(404).json({
          success: false,
          message: 'Token not found for this user in the queue.'
        });
      }

      return res.status(400).json({
        success: false,
        message: `Cannot leave queue. Your token status is already ${existingToken.status}.`
      });
    }

    // 3. Update the token status to CANCELLED (as defined in your schema enum)
    queue.tokens[tokenIndex].status = 'CANCELLED';
    await queue.save();

    console.log(`User ${userId} left queue ${queueId}, token #${queue.tokens[tokenIndex].tokenNumber} marked as CANCELLED`);

    return res.status(200).json({
      success: true,
      message: 'Successfully left the queue.',
      data: {
        queueId: queue._id,
        tokenNumber: queue.tokens[tokenIndex].tokenNumber,
        status: 'CANCELLED'
      }
    });

  } catch (error) {
    console.error("Error in leaveQueue:", error);
    return res.status(500).json({
      success: false,
      error: error.message
    });
  }
};
const getPendingFeedback = async (req, res) => {
  try {
    const userId = oid(req.user.id);

    const result = await QueueV2.aggregate([
      { $match: { "tokens.userId": userId } },
      { $unwind: "$tokens" },
      { $match: {
          "tokens.userId": userId,
          "tokens.status": "COMPLETED",
          "tokens.feedback.status": { $nin: ['SUBMITTED', 'SKIPPED'] }
      } },
      { $sort: { "tokens.consultationCompletedAt": -1 } },
      { $limit: 1 },
      { $project: {
          queueId: "$_id", tokenId: "$tokens._id",
          hospitalId: 1, doctorCode: 1, tokenNumber: "$tokens.tokenNumber"
      } }
    ]);

    if (!result.length) return res.status(200).json({ success: true, data: null });
    const p = result[0];
    const isOid = (v) => typeof v === 'string' && /^[0-9a-fA-F]{24}$/.test(v);

    const [doctor, hospital] = await Promise.all([
      UserV2.findOne({
        $or: [{ doctorCode: p.doctorCode }, { _id: isOid(p.doctorCode) ? p.doctorCode : null }],
        role: 'DOCTOR'
      }).select('name').lean(),
      HospitalV2.findOne({
        $or: [{ code: p.hospitalId }, { _id: isOid(p.hospitalId) ? p.hospitalId : null }]
      }).select('name').lean()
    ]);

    const rawName = doctor?.name ? doctor.name.replace(/^dr\.?\s*/i, '') : null;

    return res.status(200).json({
      success: true,
      data: {
        queueId: p.queueId,
        tokenId: p.tokenId,
        tokenNumber: p.tokenNumber,
        doctorName: rawName ? `Dr. ${rawName}` : "your doctor",
        hospitalName: hospital?.name || "the hospital"
      }
    });
  } catch (error) {
    return res.status(500).json({ success: false, error: error.message });
  }
};

const saveFeedback = async (req, res, feedback) => {
  const { queueId, tokenId } = req.body;
  if (!queueId || !tokenId) {
    return res.status(400).json({ success: false, message: 'queueId and tokenId are required.' });
  }

  const result = await QueueV2.updateOne(
    { _id: queueId },
    { $set: { "tokens.$[t].feedback": { ...feedback, respondedAt: new Date() } } },
    { arrayFilters: [{
        "t._id": oid(tokenId),
        "t.userId": oid(req.user.id),
        "t.status": "COMPLETED",
        "t.feedback.status": { $exists: false }
    }] }
  );

  if (result.modifiedCount === 0) {
    return res.status(409).json({ success: false, message: 'Not allowed or already answered.' });
  }
  return res.status(200).json({ success: true });
};

const submitFeedback = async (req, res) => {
  try {
    const { doctorRating, doctorComment, hospitalRating, hospitalComment } = req.body;
    if (!(doctorRating >= 1 && doctorRating <= 5) || !(hospitalRating >= 1 && hospitalRating <= 5)) {
      return res.status(400).json({ success: false, message: 'Ratings must be between 1 and 5.' });
    }
    return await saveFeedback(req, res, {
      status: 'SUBMITTED',
      doctorRating,
      doctorComment: (doctorComment || "").trim().slice(0, 500),
      hospitalRating,
      hospitalComment: (hospitalComment || "").trim().slice(0, 500)
    });
  } catch (error) {
    return res.status(500).json({ success: false, error: error.message });
  }
};

const skipFeedback = async (req, res) => {
  try {
    return await saveFeedback(req, res, { status: 'SKIPPED' });
  } catch (error) {
    return res.status(500).json({ success: false, error: error.message });
  }
};
module.exports={
    createDepartmentQueue,
    getLiveQueueTicket,
    leaveQueue,
    getUserQueuesDashboard,
    getPendingFeedback,
    saveFeedback,
    skipFeedback
}
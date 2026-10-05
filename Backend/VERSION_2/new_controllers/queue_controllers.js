const QueueV2 = require('../new_models/new_queuev2');
const UserV2 = require('../new_models/peron_model');
const HospitalV2 = require('../new_models/new_hosp_model');
const UserActiveQueue = require('../new_models/new_user_active_queue');
const QueueHistory = require('../new_models/new_user_queue_history');
const socket = require("../../socket");
const {calculateETA}=require('../new_controllers/eta_control')
const mongoose = require('mongoose');
const oid = (v) => new mongoose.Types.ObjectId(v);
const {
    sendNotification
} = require("../new_controllers/notification_controller");
const createDepartmentQueue = async (req, res) => {
  try {
    console.log("🔥 CREATE QUEUE HIT");

    const {
      hospitalId,
      department,
      doctorCode,
      queueStatus
    } = req.body;

    // ---------------------------------------
    // 1. VALIDATE INPUT
    // ---------------------------------------

    if (!hospitalId || !department || !doctorCode) {
      return res.status(400).json({
        success: false,
        message: "hospitalId, department and doctorCode are required."
      });
    }

    // ---------------------------------------
    // 2. VERIFY DOCTOR
    // ---------------------------------------

    const doctor = await UserV2.findOne({
      doctorCode,
      hospitalId,
      role: "DOCTOR"
    });

    if (!doctor) {
      return res.status(404).json({
        success: false,
        message: "Doctor not found in this hospital."
      });
    }

    // ---------------------------------------
    // 3. VERIFY DEPARTMENT
    // ---------------------------------------

    if (!doctor.department.includes(department)) {
      return res.status(400).json({
        success: false,
        message: `Doctor belongs to the ${doctor.department.join(", ")} department, not ${department}.`
      });
    }

    const today = new Date()
      .toISOString()
      .split("T")[0];

    // ---------------------------------------
    // 4. FIND EXISTING QUEUE
    // ONE QUEUE PER DOCTOR PER HOSPITAL
    // ---------------------------------------

    let queue = await QueueV2.findOne({
      hospitalId,
      doctorCode
    });

    // ---------------------------------------
    // 5. IF QUEUE ALREADY EXISTS
    // REUSE IT FOR DEMO
    // ---------------------------------------

    if (queue) {

      console.log(
        "Existing queue found:",
        queue._id.toString()
      );

      // Remove old active queue records
      await UserActiveQueue.deleteMany({
        queueId: queue._id
      });

      // Reset queue for new session
      queue.department = department;
      queue.date = today;

      queue.avgServiceTime = 5;

      queue.tokens = [];

      queue.queueStatus =
        queueStatus || "ACTIVE";

      queue.isActive = true;

      await queue.save();

      console.log(
        "♻️ Existing queue reused:",
        queue._id.toString()
      );

    } else {

      // ---------------------------------------
      // 6. CREATE FIRST QUEUE
      // ---------------------------------------

      queue = await QueueV2.create({
        hospitalId,
        department,
        doctorCode,
        date: today,
        avgServiceTime: 5,
        queueStatus: queueStatus || "ACTIVE",
        isActive: true,
        tokens: []
      });

      console.log(
        "🆕 New queue created:",
        queue._id.toString()
      );
    }

    // ---------------------------------------
    // 7. SOCKET EVENT
    // ---------------------------------------

    const io = socket.getIO();

    io.emit("QUEUE_CREATED", {
      queueId: queue._id.toString(),
      hospitalId,
      department,
      doctorCode,
      queueStatus: queue.queueStatus,
      isActive: queue.isActive
    });

    console.log(
      "QUEUE_CREATED emitted:",
      queue._id.toString()
    );

    // ---------------------------------------
    // 8. ADMIN NOTIFICATION
    // ---------------------------------------

    try {
      await sendNotification({
        hospitalId,
        targetRole: "ADMIN",
        title: "Queue Started",
        message: `${department} queue has been started by Dr. ${doctor.name}.`,
        type: "QUEUE_STARTED",
        department,
        doctorCode
      });
    } catch (notificationError) {

      // Don't fail queue creation if notification fails
      console.error(
        "Notification failed:",
        notificationError.message
      );
    }

    // ---------------------------------------
    // 9. SUCCESS RESPONSE
    // ---------------------------------------

    return res.status(200).json({
      success: true,
      message: "Department queue started successfully.",
      data: queue
    });

  } catch (error) {

    console.error(
      " Error in createDepartmentQueue:",
      error
    );

    return res.status(500).json({
      success: false,
      message: "Failed to create/start queue.",
      error: error.message
    });
  }
};
const getLiveQueueTicket = async (req, res) => {
    try {

        const { queueId } = req.query;
        const userId = req.params.userId;

        console.log("User:", userId);
        console.log("Queue:", queueId);

        if (!queueId || !userId) {
            return res.status(400).json({
                success: false,
                message: "queueId and userId are required."
            });
        }

        // ---------------------------------------
        // 1. FIND USER'S ACTIVE QUEUE
        // ---------------------------------------

        const activeQueue = await UserActiveQueue.findOne({
            userId: userId,
            queueId: queueId
        }).lean();

        if (!activeQueue) {
            return res.status(404).json({
                success: false,
                message: "User is not active in this queue."
            });
        }

        // ---------------------------------------
        // 2. FETCH LIVE QUEUE
        // ---------------------------------------

        const queue = await QueueV2.findById(queueId)
            .lean();

        if (!queue) {
            return res.status(404).json({
                success: false,
                message: "Queue session not found."
            });
        }

        // ---------------------------------------
        // 3. FIND USER'S TOKEN
        // ---------------------------------------

        const userToken = queue.tokens.find(
            token =>
                token._id?.toString() ===
                activeQueue.tokenId?.toString()
        );

        if (!userToken) {
            return res.status(404).json({
                success: false,
                message: "User token not found in queue."
            });
        }

        console.log(
            "User token:",
            userToken.tokenNumber
        );

        console.log(
            "User notes:",
            userToken.notes
        );

        // ---------------------------------------
        // 4. FIND HOSPITAL + DOCTOR
        // ---------------------------------------

        const [hospital, doctor] = await Promise.all([

            HospitalV2.findOne({
                code: queue.hospitalId
            }).lean(),

            UserV2.findOne({
                doctorCode: queue.doctorCode,
                role: "DOCTOR"
            }).lean()

        ]);

        // ---------------------------------------
        // 5. DOCTOR NAME
        // ---------------------------------------

        const rawName = doctor?.name
            ? doctor.name.replace(/^dr\.?\s*/i, "")
            : queue.doctorCode;

        const doctorDisplayName =
            rawName
                ? `Dr. ${rawName}`
                : (queue.doctorCode || "Doctor");

        // ---------------------------------------
        // 6. CALCULATE ETA
        // ---------------------------------------

        const etaData = await calculateETA(
            queue,
            activeQueue.tokenNumber
        );

        const peopleAhead =
            etaData.peopleAhead || 0;

        const estWaitTime =
            etaData.eta || 0;

        // ---------------------------------------
        // 7. QUEUE MESSAGE
        // ---------------------------------------

        let queueMessage =
            "Please wait for your turn.";

        if (
            activeQueue.status === "IN_CONSULTATION"
        ) {

            queueMessage =
                "You are currently in consultation.";

        } else if (
            peopleAhead === 0
        ) {

            queueMessage =
                "You are next! Please proceed near Room 04";

        } else {

            queueMessage =
                `${peopleAhead} people ahead of you. ` +
                `Estimated wait: ${estWaitTime} min`;
        }

        // ---------------------------------------
        // 8. RESPONSE
        // ---------------------------------------

        return res.status(200).json({

            success: true,

            data: {

                hospitalName:
                    hospital?.name ||
                    "APOLLO HOSPITAL",

                hospitalLogoUrl:
                    hospital?.logoUrl || "",

                notes:
                    userToken.notes || "",

                doctorName:
                    doctorDisplayName,

                department:
                    queue.department ||
                    "Emergency Department",

                roomNumber:
                    doctor?.roomNumber ||
                    "Room 04",

                isDoctorOnDuty:
                    queue.queueStatus === "ACTIVE",

                tokenNumber:
                    activeQueue.tokenNumber,

                status:
                    activeQueue.status,

                queueMessage:
                    queueMessage,

                peopleAheadText:
                    `${peopleAhead} people`,

                estWaitTimeText:
                    `${estWaitTime} min`,

                queueDate:
                    queue.date,

                isPaid:
                    activeQueue.amountPaid > 0,

                paymentText:
                    activeQueue.amountPaid > 0
                        ? "✓ Paid"
                        : "Pending"
            }
        });

    } catch (error) {

        console.error(
            "Error fetching live queue ticket:",
            error
        );

        return res.status(500).json({
            success: false,
            error: error.message
        });
    }
};
const getUserQueuesDashboard = async (req, res) => {
    try {
        console.log("uerqueuedashboardhit");

        const { userId } = req.params;

        if (!userId) {
            return res.status(400).json({
                success: false,
                message: "userId is required"
            });
        }

        // ==========================================
        // 1. GET ACTIVE QUEUES
        // ==========================================

        const activeQueues = await UserActiveQueue.find({
            userId: userId
        })
        .sort({ createdAt: -1 })
        .lean();


        // ==========================================
        // 2. GET RECENT HISTORY
        // ==========================================

        const history = await QueueHistory.find({
            userId: userId
        })
        .sort({ createdAt: -1 })
        .limit(5)
        .lean();


        const activeQueueList = [];


        // ==========================================
        // 3. ACTIVE QUEUE DETAILS
        // ==========================================

        for (const active of activeQueues) {

            const queue = await QueueV2.findById(active.queueId)
                .select(
                    "queueStatus avgServiceTime tokens date createdAt department hospitalId doctorCode"
                )
                .lean();

            if (!queue) continue;


            // ==========================================
            // LIVE ETA
            // ==========================================

            const etaData = await calculateETA(
                queue,
                active.tokenNumber
            );

            const peopleAhead =
                etaData.peopleAhead || 0;

            const estWaitTime =
                etaData.eta || 0;


            // ==========================================
            // ADD ACTIVE QUEUE
            // ==========================================

            activeQueueList.push({

                queueId:
                    active.queueId,

                tokenId:
                    active.tokenId,

                tokenNumber:
                    active.tokenNumber,


                // --------------------------
                // Hospital
                // --------------------------

                hospitalId:
                    active.hospitalId,

                hospitalName:
                    active.hospitalName || "Hospital",

                hospitalLogoUrl:
                    active.hospitalLogoUrl || "",


                // --------------------------
                // Doctor
                // --------------------------

                doctorCode:
                    active.doctorCode,

                doctorName:
                    active.doctorName || active.doctorCode,


                // --------------------------
                // Queue
                // --------------------------

                department:
                    active.department || "",

                roomNumber:
                    active.roomNumber || "",

                date:
                    active.date || queue.date || "",


                // --------------------------
                // Status
                // --------------------------

                status:
                    active.status,

                queueStatus:
                    queue.queueStatus,


                // --------------------------
                // Time
                // --------------------------

                createdAt:
                    active.createdAt,

                peopleAheadText:
                    `${peopleAhead} people ahead`,

                estWaitTimeText:
                    `${estWaitTime} min`
            });
        }


        // ==========================================
        // 4. HISTORY
        // ==========================================

        const historyList = history.map(item => ({

            queueId:
                item.queueId,

            tokenId:
                item.tokenId,

            tokenNumber:
                item.tokenNumber,


            // --------------------------
            // Hospital
            // --------------------------

            hospitalName:
                item.hospitalName || "Hospital",

            hospitalLogoUrl:
                item.hospitalLogoUrl || "",


            // --------------------------
            // Doctor
            // --------------------------

            doctorCode:
                item.doctorCode,

            doctorName:
                item.doctorName || "",


            // --------------------------
            // Queue
            // --------------------------

            department:
                item.department || "",

            roomNumber:
                item.roomNumber || "",


            // --------------------------
            // Status
            // --------------------------

            status:
                item.status,


            // --------------------------
            // Date
            // --------------------------

            date:
                item.date,

            createdAt:
                item.createdAt,


            // --------------------------
            // Feedback
            // --------------------------

            feedbackStatus:
                item.feedback?.status || null,


            // --------------------------
            // Display strings
            // --------------------------

            doctorDetails:
                `${item.department || ""} • Dr. ${item.doctorName || ""}`,

            subText:
                `${item.department || ""} • Dr. ${item.doctorName || ""} (${item.date || ""})`
        }));


        // ==========================================
        // 5. FINAL RESPONSE
        // ==========================================

        return res.status(200).json({

            success: true,

            data: {

                activeQueue:
                    activeQueueList,

                recentHistory:
                    historyList
            }
        });

    } catch (error) {

        console.error(
            "Error fetching user dashboard:",
            error
        );

        return res.status(500).json({
            success: false,
            error: error.message
        });
    }
};
const leaveQueue = async (req, res) => {
  try {
    const { queueId } = req.body;
    const userId = req.user.id;

    if (!queueId || !userId) {
      return res.status(400).json({
        success: false,
        message: 'queueId and userId are required.'
      });
    }

    // 1. Find the queue
    const queue = await QueueV2.findById(queueId);

    if (!queue) {
      return res.status(404).json({
        success: false,
        message: 'Queue session not found.'
      });
    }

    // 2. Find user's WAITING token
    const tokenIndex = queue.tokens.findIndex(
      t =>
        t.userId.toString() === userId.toString() &&
        t.status === 'WAITING'
    );

    if (tokenIndex === -1) {

      const existingToken = queue.tokens.find(
        t => t.userId.toString() === userId.toString()
      );

      if (!existingToken) {
        return res.status(404).json({
          success: false,
          message: 'Token not found for this user in the queue.'
        });
      }

      return res.status(400).json({
        success: false,
        message:
          `Cannot leave queue. Your token status is already ${existingToken.status}.`
      });
    }

    // 3. Store token before changing anything
    const token = queue.tokens[tokenIndex];

    // 4. Mark token as CANCELLED in QueueV2
    token.status = 'CANCELLED';

    await queue.save();

    // 5. Create history record
   // 5. Fetch hospital + doctor details for history snapshot

const [hospital, doctor] = await Promise.all([

  HospitalV2.findOne({
    $or: [
      { code: queue.hospitalId },
      {
        _id:
          queue.hospitalId &&
          queue.hospitalId.toString().match(/^[0-9a-fA-F]{24}$/)
            ? queue.hospitalId
            : null
      }
    ]
  }).lean(),

  UserV2.findOne({
    $or: [
      { doctorCode: queue.doctorCode },
      { code: queue.doctorCode },
      {
        _id:
          queue.doctorCode &&
          queue.doctorCode.toString().match(/^[0-9a-fA-F]{24}$/)
            ? queue.doctorCode
            : null
      }
    ],
    role: "DOCTOR"
  }).lean()

]);

// Remove "Dr." if already present
const doctorName = doctor?.name
  ? doctor.name.replace(/^dr\.?\s*/i, '')
  : queue.doctorCode;


// 6. Create history snapshot

await QueueHistory.create({

  userId: userId,

  queueId: queue._id,

  tokenId: token._id,

  tokenNumber: token.tokenNumber,

  // Hospital
  hospitalId: queue.hospitalId,

  hospitalName:
    hospital?.name || "Hospital",

  hospitalLogoUrl:
    hospital?.logoUrl || "",

  // Doctor
  doctorCode: queue.doctorCode,

  doctorName: doctorName,

  roomNumber:
    doctor?.roomNumber || "",

  // Queue
  department: queue.department,

  date: queue.date,

  status: "CANCELLED",

  // Time
  joinedAt:
    token.joinedAt || queue.createdAt,

  completedAt:
    new Date(),

  feedback: {
    status: "PENDING"
  }

});
    // 6. Remove user from active queues
    await UserActiveQueue.deleteOne({
      userId: userId,
      queueId: queue._id,
      tokenId: token._id
    });

    console.log(
      `User ${userId} left queue ${queueId}, ` +
      `token #${token.tokenNumber} marked as CANCELLED`
    );

    // 7. Response
    return res.status(200).json({
      success: true,

      message: 'Successfully left the queue.',

      data: {
        queueId: queue._id,
        tokenId: token._id,
        tokenNumber: token.tokenNumber,
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
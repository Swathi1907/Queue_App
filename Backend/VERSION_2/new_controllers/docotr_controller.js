const UserV2 = require('../new_models/peron_model');
const HospitalV2 = require('../new_models/new_hosp_model');
const Queue = require("../new_models/new_queuev2");
const socket =require('../../socket');
const UserActiveQueue = require("../new_models/new_user_active_queue");
const QueueHistory = require("../new_models/new_user_queue_history");
const { calculateETA } = require("../new_controllers/eta_control");
const {
    sendNotification
} = require("../new_controllers/notification_controller");
// 1. Fetch Assigned Doctor Profile and Department Details
const getDoctorProfile = async (req, res) => {
  try {
    const doctorId = req.user.id;
    const doctor = await UserV2.findById(doctorId).select('-password');

    if (!doctor) {
      return res.status(404).json({
        success: false,
        message: "Doctor profile not found.",
      });
    }

    let departmentList = doctor.department;
    if (!Array.isArray(departmentList)) {
      departmentList = departmentList ? [departmentList] : [];
    }

    return res.status(200).json({
      success: true,
      data: {
        _id: doctor._id,
        name: doctor.name,
        email: doctor.email,
        department: departmentList,
        hospitalId: doctor.hospitalId,
        rating: doctor.rating,
        phoneNumber: doctor.phoneNumber,
        doctorCode: doctor.doctorCode,
        qualification: doctor.qualification,
        isAvailable: doctor.isAvailable
      }
    });
  } catch (error) {
    console.log(error.message);
    return res.status(500).json({ success: false, error: error.message });
  }
};

const session_there = async (req, res) => {
    try {
        const { department, doctorCode } = req.query;

        if (!doctorCode || !department) {
            return res.status(400).json({
                success: false,
                message: "doctorCode and department parameters are required"
            });
        }

        const queueDoc = await Queue.findOne({
            doctorCode: doctorCode,
            department: department,
            queueStatus: { $ne: 'CLOSED' }
        });

        if (!queueDoc) {
            console.log("not found")
            return res.status(200).json({
                success: true,
                message: "No active queue found for today",
                data: null
            });
        }

        return res.status(200).json({
            success: true,
            message: "Active session retrieved successfully",
            data: {
                sessionId: queueDoc._id,
                queueStatus: queueDoc.queueStatus,
                avgServiceTime: queueDoc.avgServiceTime || 5, // Exposed here
                tokens: queueDoc.tokens
            }
        });

    } catch (error) {
        console.error("Error fetching active session:", error);
        return res.status(500).json({
            success: false,
            message: "Internal server error"
        });
    }
};

const next = async (req, res) => {
    try {
        const { department, doctorCode } = req.body;

        if (!doctorCode || !department) {
            return res.status(400).json({
                success: false,
                message: "doctorCode and department parameters are required"
            });
        }

        const query = { doctorCode, department, queueStatus: { $ne: 'CLOSED' } };
        const queueDoc = await Queue.findOne(query);

        if (!queueDoc) {
            return res.status(404).json({ success: false, message: "Active queue session not found" });
        }

        const existingActive = queueDoc.tokens.find(t => t.status === 'IN_CONSULTATION');
        if (existingActive) {
            return res.status(400).json({
                success: false,
                message: "A patient is already in consultation. Complete the current consultation first."
            });
        }

        if (queueDoc.queueStatus === 'PAUSED') {
            return res.status(404).json({
                success: false,
                message: "Cannot call next patient while the queue is paused."
            });
        }

        const nextToken = queueDoc.tokens.find(t => t.status === 'WAITING');
    
        if (!nextToken) {
            return res.status(404).json({
                success: false,
                message: "No waiting patients found in the queue"
            });
        }

     nextToken.status = 'IN_CONSULTATION';
nextToken.consultationStartedAt = new Date();
        await queueDoc.save();
const io = socket.getIO();

io.to(`queue_${queueDoc._id}`).emit("TOKEN_CALLED", {
    queueId: queueDoc._id,
    tokenId: nextToken._id,
    tokenNumber: nextToken.tokenNumber,
    status: "IN_CONSULTATION"
});
        // Calculate dynamic ETA metrics for the newly called token
        const etaData = await calculateETA(queueDoc, nextToken.tokenNumber);

        return res.status(200).json({
            success: true,
            message: "Next patient called successfully",
            data: {
                sessionId: queueDoc._id,
                queueStatus: queueDoc.queueStatus,
            avgServiceTime: Math.ceil(queueDoc.avgServiceTime || 5),
                calledToken: nextToken,
                metrics: etaData, // Full breakdown including progress, activeCount, remaining times
                tokens: queueDoc.tokens
            }
        });

    } catch (error) {
        console.error("Error calling next patient:", error);
        return res.status(500).json({ success: false, message: "Internal server error" });
    }
};
const completeCurrent = async (req, res) => {
    try {
        const { department, doctorCode } = req.body;

        if (!doctorCode || !department) {
            return res.status(400).json({
                success: false,
                message: "doctorCode and department parameters are required"
            });
        }

        const query = {
            doctorCode,
            department,
            queueStatus: { $ne: 'CLOSED' }
        };

        const queueDoc = await Queue.findOne(query);

        if (!queueDoc) {
            return res.status(404).json({
                success: false,
                message: "Active queue session not found"
            });
        }

        // ==========================================
        // 1. FIND CURRENT PATIENT
        // ==========================================

        const activeToken = queueDoc.tokens.find(
            t => t.status === 'IN_CONSULTATION'
        );

        if (!activeToken) {
            return res.status(404).json({
                success: false,
                message: "No patient currently in consultation to complete"
            });
        }


        // ==========================================
        // 2. FIND USER ACTIVE QUEUE
        // ==========================================

        const activeQueue = await UserActiveQueue.findOne({
            queueId: queueDoc._id,
            tokenId: activeToken._id
        });

        if (!activeQueue) {
            return res.status(404).json({
                success: false,
                message: "Active queue record not found for this patient"
            });
        }


        // ==========================================
        // 3. CALCULATE ACTUAL SERVICE TIME
        // ==========================================

        const completedAt = new Date();

        let serviceTime = null;

        if (activeToken.consultationStartedAt) {

            serviceTime =
                (
                    completedAt.getTime() -
                    new Date(
                        activeToken.consultationStartedAt
                    ).getTime()
                ) / (1000 * 60);

            serviceTime =
                Number(serviceTime.toFixed(2));
        }


        // ==========================================
        // 4. MARK TOKEN AS COMPLETED
        // ==========================================

        activeToken.status = 'COMPLETED';

        activeToken.consultationCompletedAt =
            completedAt;

        activeToken.serviceTime =
            serviceTime;


        // ==========================================
        // 5. RECALCULATE AVERAGE SERVICE TIME
        // ==========================================

        const completedTokens =
            queueDoc.tokens.filter(
                token =>
                    token.status === 'COMPLETED' &&
                    token.serviceTime != null
            );

        if (completedTokens.length > 0) {

            const totalServiceTime =
                completedTokens.reduce(
                    (sum, token) =>
                        sum + token.serviceTime,
                    0
                );

            queueDoc.avgServiceTime = Math.ceil(
    totalServiceTime / completedTokens.length
);
        }


        // ==========================================
        // 6. SAVE QUEUE
        // ==========================================

        await queueDoc.save();


        // ==========================================
        // 7. CREATE QUEUE HISTORY
        // ==========================================

    
    // ==========================================
// 7. GET HOSPITAL + DOCTOR DETAILS
// ==========================================
const hospital = await HospitalV2.findOne({
    code: queueDoc.hospitalId
}).lean();

const doctor = await UserV2.findOne({
    doctorCode: queueDoc.doctorCode,
    role: "DOCTOR"
}).lean();


// ==========================================
// 8. CREATE QUEUE HISTORY
// ==========================================
const history = new QueueHistory({

    userId: activeQueue.userId,

    queueId: queueDoc._id,

    tokenId: activeToken._id,

    tokenNumber: activeToken.tokenNumber,

    hospitalId: queueDoc.hospitalId,

    hospitalName:
        hospital?.name || "",

    hospitalLogoUrl:
        hospital?.logoUrl || "",

    doctorCode:
        queueDoc.doctorCode,

    doctorName:
        doctor?.name || "",

    department:
        queueDoc.department,

    roomNumber:
        queueDoc.roomNumber || "",

    status: "COMPLETED",

    date:
        queueDoc.date,

    feedback:
        activeToken.feedback || {},

    joinedAt:
        activeQueue.joinedAt,

    completedAt:
        completedAt
});



await history.save();
        // ==========================================
        // 8. DELETE FROM USER ACTIVE QUEUE
        // ==========================================

        await UserActiveQueue.deleteOne({
            _id: activeQueue._id
        });


        // ==========================================
        // 9. FIND NEXT WAITING PATIENT
        // ==========================================

        const nextWaitingToken =
            queueDoc.tokens.find(
                t => t.status === 'WAITING'
            );


        // ==========================================
        // 10. CALCULATE NEXT PATIENT ETA
        // ==========================================

        let nextMetrics = null;

        if (nextWaitingToken) {

            nextMetrics =
                await calculateETA(
                    queueDoc,
                    nextWaitingToken.tokenNumber
                );
        }


        // ==========================================
        // 11. SOCKET EVENT
        // ==========================================

        const io = socket.getIO();

        io.to(`queue_${queueDoc._id}`).emit(
            "TOKEN_COMPLETED",
            {
                queueId: queueDoc._id,
                tokenId: activeToken._id,
                tokenNumber: activeToken.tokenNumber,
                status: "COMPLETED",
                nextTokenNumber:
                    nextWaitingToken?.tokenNumber || null,
                metrics: nextMetrics
            }
        );


        // ==========================================
        // 12. RESPONSE
        // ==========================================

        return res.status(200).json({

            success: true,

            message:
                "Current consultation completed successfully",

            data: {

                sessionId:
                    queueDoc._id,

                queueStatus:
                    queueDoc.queueStatus,

                avgServiceTime:
                    queueDoc.avgServiceTime,

                serviceTime:
                    serviceTime,

                completedToken:
                    activeToken,

                historyId:
                    history._id,

                nextMetrics:
                    nextMetrics,

                tokens:
                    queueDoc.tokens
            }
        });

    } catch (error) {

        console.error(
            "Error completing current consultation:",
            error
        );

        return res.status(500).json({
            success: false,
            message: "Internal server error"
        });
    }
};

const updateQueueStatus = async (req, res) => {
    try {
        const { department, doctorCode, queueStatus } = req.body;

        if (!doctorCode || !department || !queueStatus) {
            return res.status(400).json({
                success: false,
                message: "doctorCode, department, and queueStatus parameters are required"
            });
        }

        const validStatuses = ['ACTIVE', 'PAUSED', 'CLOSED'];
        if (!validStatuses.includes(queueStatus)) {
            return res.status(400).json({
                success: false,
                message: `Invalid queueStatus value. Allowed values are: ${validStatuses.join(', ')}`
            });
        }

        const query = { doctorCode, department, queueStatus: { $ne: 'CLOSED' } };
        const queueDoc = await Queue.findOne(query);

        if (!queueDoc) {
            return res.status(404).json({ 
                success: false, 
                message: "Active queue session not found" 
            });
        }

        if (queueStatus === 'PAUSED') {
            const activeConsultation = queueDoc.tokens.find(t => t.status === 'IN_CONSULTATION');
            if (activeConsultation) {
                return res.status(400).json({
                    success: false,
                    message: "Please complete the current consultation before pausing the queue."
                });
            }
        }

        queueDoc.queueStatus = queueStatus;
        queueDoc.isActive = (queueStatus === 'ACTIVE');
       await queueDoc.save();

const io = socket.getIO();

const roomName = `queue_${queueDoc._id}`;

console.log("=================================");
console.log("QUEUE STATUS UPDATED");
console.log("Queue ID:", queueDoc._id.toString());
console.log("New Status:", queueDoc.queueStatus);
console.log("Room:", roomName);

const room = io.sockets.adapter.rooms.get(roomName);

console.log(
    "Sockets in room:",
    room ? [...room] : "NO SOCKETS"
);

io.to(roomName).emit("QUEUE_STATUS_CHANGED", {
    queueId: queueDoc._id.toString(),
    queueStatus: queueDoc.queueStatus,
    isActive: queueDoc.isActive
});

console.log("QUEUE_STATUS_CHANGED emitted");
console.log("=================================");
await sendNotification({
            hospitalId: queueDoc.hospitalId,
            targetRole: 'ADMIN',
            title: "Queue Status Updated",
            message: `Doctor ${doctorCode} (${department}) changed queue status to ${queueStatus}.`,
            type: "QUEUE_STATUS_UPDATE",
            department,
            doctorCode
        });
        return res.status(200).json({
            success: true,
            message: `Queue status updated to ${queueStatus} successfully`,
            data: {
                sessionId: queueDoc._id,
                queueStatus: queueDoc.queueStatus,
                avgServiceTime: queueDoc.avgServiceTime || 5,
                isActive: queueDoc.isActive,
                tokens: queueDoc.tokens
            }
        });

    } catch (error) {
        console.error("Error updating queue status:", error);
        return res.status(500).json({ success: false, message: "Internal server error" });
    }
};const end_session = async (req, res) => {
    try {
        const { department, doctorCode } = req.query;

        if (!doctorCode || !department) {
            return res.status(400).json({
                success: false,
                message: "doctorCode and department parameters are required"
            });
        }

        const queueDoc = await Queue.findOne({
            doctorCode: doctorCode,
            department: department,
            queueStatus: { $ne: 'CLOSED' }
        });

        if (!queueDoc) {
            return res.status(404).json({
                success: false,
                message: "No active session found"
            });
        }

        // Check for patients who are still active
        const activeTokens = queueDoc.tokens.filter(token =>
            token.status === 'WAITING' ||
            token.status === 'IN_CONSULTATION'
        );

        if (activeTokens.length > 0) {
            return res.status(400).json({
                success: false,
                message: `Cannot end session. ${activeTokens.length} patient(s) are still in the queue.`,
                activePatients: activeTokens.length
            });
        }

        // Close the queue
        queueDoc.queueStatus = 'CLOSED';
        queueDoc.isActive = false;

        await queueDoc.save();

        // 🔥 REAL-TIME SOCKET EVENT
        const io = socket.getIO();

        io.to(`queue_${queueDoc._id}`).emit("QUEUE_SESSION_ENDED", {
            queueId: queueDoc._id.toString(),
            queueStatus: "CLOSED",
            isActive: false
        });

        console.log(
            `QUEUE_SESSION_ENDED emitted to queue_${queueDoc._id}`
        );

        await sendNotification({
            hospitalId: queueDoc.hospitalId,
            targetRole: 'ADMIN',
            title: "Queue Session Closed",
            message: `Doctor ${doctorCode} (${department}) has closed their queue session.`,
            type: "QUEUE_CLEARED",
            department,
            doctorCode
        });

        return res.status(200).json({
            success: true,
            message: "Session ended successfully",
            data: {
                sessionId: queueDoc._id,
                queueStatus: queueDoc.queueStatus
            }
        });

    } catch (error) {
        console.error("Error ending session:", error);

        return res.status(500).json({
            success: false,
            message: "Internal server error"
        });
    }
};
const getDoctorAnalytics = async (req, res) => {
    try {
        const {
            hospitalId,
            department,
            doctorCode
        } = req.query;

        if (!hospitalId || !department || !doctorCode) {
            return res.status(400).json({
                success: false,
                message:
                    "hospitalId, department and doctorCode are required"
            });
        }

        const decodedDepartment =
            decodeURIComponent(department);

        // Find the specific doctor
        const doctor = await UserV2.findOne({
            hospitalId,
            role: "DOCTOR",
            doctorCode: doctorCode,
            department: decodedDepartment
        })
        .select("name doctorCode department")
        .lean();

        if (!doctor) {
            return res.status(404).json({
                success: false,
                message: "Doctor not found"
            });
        }

        // Get ALL queues of this doctor in this department
        // including CLOSED queues because they contain history
        const queues = await Queue.find({
            hospitalId,
            department: decodedDepartment,
            doctorCode: doctorCode
        }).lean();

        let patientsTreated = 0;

        const serviceTimes = [];
        const volumeMap = {};
        const hourMap = {};

        // --------------------------------
        // Process all queues of this doctor
        // --------------------------------

        queues.forEach(queue => {

            const tokens = Array.isArray(queue.tokens)
                ? queue.tokens
                : [];

            tokens.forEach(token => {

                // Patients treated
                if (token.status === "COMPLETED") {

                    patientsTreated++;

                    if (
                        typeof token.serviceTime === "number" &&
                        token.serviceTime > 0
                    ) {
                        serviceTimes.push(
                            token.serviceTime
                        );
                    }
                }

                // Patient volume
                if (token.createdAt) {

                    const date =
                        new Date(token.createdAt)
                            .toISOString()
                            .split("T")[0];

                    volumeMap[date] =
                        (volumeMap[date] || 0) + 1;

                    // Peak hour
                    const hour =
                        new Date(token.createdAt)
                            .getHours();

                    hourMap[hour] =
                        (hourMap[hour] || 0) + 1;
                }
            });
        });

        // --------------------------------
        // Average service time
        // --------------------------------

        let averageServiceTime = 0;

        if (serviceTimes.length > 0) {

            const total =
                serviceTimes.reduce(
                    (sum, time) => sum + time,
                    0
                );

            averageServiceTime =
                Math.round(
                    (total / serviceTimes.length) * 10
                ) / 10;
        }

        // --------------------------------
        // Patient volume trend
        // --------------------------------

        const patientVolumeTrend =
            Object.entries(volumeMap)
                .sort(([a], [b]) =>
                    a.localeCompare(b)
                )
                .slice(-7)
                .map(([date, count]) => ({
                    date,
                    count
                }));

        // --------------------------------
        // Peak hours
        // --------------------------------

        const peakHours =
            Object.entries(hourMap)
                .sort(
                    ([, a], [, b]) => b - a
                )
                .slice(0, 5)
                .map(([hour, count]) => ({
                    hour: Number(hour),
                    count
                }));

        return res.status(200).json({
            success: true,

            data: {
                doctorCode: doctor.doctorCode,
                doctorName: doctor.name,
                department: decodedDepartment,

                patientsTreated,
                averageServiceTime,

                patientVolumeTrend,
                peakHours
            }
        });

    } catch (error) {

        console.error(
            "Error fetching doctor analytics:",
            error
        );

        return res.status(500).json({
            success: false,
            message: "Internal server error"
        });
    }
};
module.exports = {
  getDoctorProfile,
  session_there,
  next,
  completeCurrent,
  updateQueueStatus,
  end_session,
  getDoctorAnalytics
};
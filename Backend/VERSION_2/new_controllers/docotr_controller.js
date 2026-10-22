const UserV2 = require('../new_models/peron_model');
const HospitalV2 = require('../new_models/new_hosp_model');
const Queue = require("../new_models/new_queuev2");
const socket =require('../../socket');
const UserActiveQueue = require("../new_models/new_user_active_queue");
const QueueHistory = require("../new_models/new_user_queue_history");
const { calculateETA } = require("../new_controllers/eta_control");


const getConsultationActivity = async (req, res) => {
    const requestId = `ACTIVITY-${Date.now()}`;

    try {
        console.log(`\n========== ${requestId} START ==========`);

        const { filter = "TODAY" } = req.query;

        console.log(`[${requestId}] Requested filter:`, filter);
        console.log(`[${requestId}] User ID:`, req.user?.id);

        // 1. Validate filter
        const allowedFilters = ["TODAY", "LAST_7_DAYS", "LAST_30_DAYS"];

        if (!allowedFilters.includes(filter)) {
            return res.status(400).json({
                success: false,
                message:
                    "Invalid filter. Use TODAY, LAST_7_DAYS, or LAST_30_DAYS",
                requestId
            });
        }

        // 2. Find authenticated doctor
        const doctor = await UserV2.findById(req.user?.id)
            .select("role hospitalId doctorCode isActive")
            .lean();

        if (!doctor || doctor.role !== "DOCTOR") {
            console.warn(`[${requestId}] Doctor not found or invalid role`);

            return res.status(403).json({
                success: false,
                message: "Only doctors can view consultation activity",
                requestId
            });
        }

        if (doctor.isActive === false) {
            return res.status(403).json({
                success: false,
                message: "Your account is inactive",
                requestId
            });
        }

        if (!doctor.hospitalId || !doctor.doctorCode) {
            return res.status(400).json({
                success: false,
                message: "Doctor hospital or doctor code is missing",
                requestId
            });
        }

        console.log(`[${requestId}] Doctor verified:`, {
            hospitalId: doctor.hospitalId,
            doctorCode: doctor.doctorCode
        });

        // 3. Calculate calendar-day boundaries in Asia/Kolkata.
        // This avoids using UTC dates as Indian calendar dates.
        const now = new Date();

        const indianDateString = new Intl.DateTimeFormat("en-CA", {
            timeZone: "Asia/Kolkata",
            year: "numeric",
            month: "2-digit",
            day: "2-digit"
        }).format(now);

        const [year, month, day] = indianDateString
            .split("-")
            .map(Number);

        // Midnight in India = previous day at 18:30 UTC.
        const todayStartUtc = new Date(
            Date.UTC(year, month - 1, day) - 330 * 60 * 1000
        );

        const days = filter === "LAST_7_DAYS" ? 7
            : filter === "LAST_30_DAYS" ? 30
            : 1;

        const startUtc = new Date(
            todayStartUtc.getTime() - (days - 1) * 24 * 60 * 60 * 1000
        );

        // Exclusive end: midnight at the start of tomorrow in India.
        const endUtc = new Date(
            todayStartUtc.getTime() + 24 * 60 * 60 * 1000
        );

        console.log(`[${requestId}] Date range:`, {
            indianDateString,
            startUtc,
            endUtc,
            days
        });

        // 4. Build the aggregation pipeline
        const groupId = filter === "TODAY"
            ? {
                $dateToString: {
                    format: "%H",
                    date: "$completedAt",
                    timezone: "Asia/Kolkata"
                }
            }
            : {
                $dateToString: {
                    format: "%Y-%m-%d",
                    date: "$completedAt",
                    timezone: "Asia/Kolkata"
                }
            };

        const pipeline = [
            {
                $match: {
                    hospitalId: doctor.hospitalId,
                    doctorCode: doctor.doctorCode,
                    status: "COMPLETED",
                    completedAt: {
                        $gte: startUtc,
                        $lt: endUtc
                    }
                }
            },
            {
                $group: {
                    _id: groupId,
                    count: { $sum: 1 },
                    averageServiceTime: { $avg: "$serviceTime" }
                }
            },
            {
                $sort: { _id: 1 }
            }
        ];

        console.log(
            `[${requestId}] Running QueueHistory aggregation...`
        );

        const results = await QueueHistory.aggregate(pipeline);

        console.log(`[${requestId}] Aggregation results:`, results);

        // 5. Index results for easy lookup
        const resultMap = new Map(
            results.map(item => [item._id, item])
        );

        let chartData = [];

        if (filter === "TODAY") {
            // Return all 24 hours, including hours with zero consultations.
            chartData = Array.from({ length: 24 }, (_, hour) => {
                const key = String(hour).padStart(2, "0");
                const item = resultMap.get(key);

                return {
                    key,
                    label: `${key}:00`,
                    consultations: item?.count || 0,
                    averageServiceTimeMinutes:
                        item?.averageServiceTime != null
                            ? Math.round(item.averageServiceTime * 100) / 100
                            : 0
                };
            });
        } else {
            // Return every calendar date in the selected range.
            chartData = Array.from({ length: days }, (_, index) => {
                const date = new Date(
                    startUtc.getTime() + index * 24 * 60 * 60 * 1000
                );

                const key = new Intl.DateTimeFormat("en-CA", {
                    timeZone: "Asia/Kolkata",
                    year: "numeric",
                    month: "2-digit",
                    day: "2-digit"
                }).format(date);

                const item = resultMap.get(key);

                return {
                    key,
                    label: key,
                    consultations: item?.count || 0,
                    averageServiceTimeMinutes:
                        item?.averageServiceTime != null
                            ? Math.round(item.averageServiceTime * 100) / 100
                            : 0
                };
            });
        }

        const totalConsultations = chartData.reduce(
            (sum, item) => sum + item.consultations,
            0
        );

        console.log(`[${requestId}] Total consultations:`, totalConsultations);
        console.log(`[${requestId}] Chart points:`, chartData.length);
        console.log(`========== ${requestId} END ==========\n`);

        return res.status(200).json({
            success: true,
            requestId,
            filter,
            timezone: "Asia/Kolkata",
            totalConsultations,
            chartData
        });

    } catch (error) {
        console.error(`[${requestId}] Consultation activity error:`, {
            message: error.message,
            stack: error.stack
        });

        return res.status(500).json({
            success: false,
            message: "Failed to fetch consultation activity",
            requestId
        });
    }
};



const getOverviewStats = async (req, res) => {
    try {
        const doctor = await UserV2.findById(req.user.id)
            .select('hospitalId doctorCode department role')
            .lean();

        if (!doctor || doctor.role !== 'DOCTOR') {
            return res.status(403).json({
                success: false,
                message: 'Doctor access only'
            });
        }

        const today = new Intl.DateTimeFormat('en-CA', {
            timeZone: 'Asia/Kolkata',
            year: 'numeric',
            month: '2-digit',
            day: '2-digit'
        }).format(new Date());

        const queues = await Queue.aggregate([
            {
                $match: {
                    hospitalId: doctor.hospitalId,
                    doctorCode: doctor.doctorCode,
                    department: { $in: doctor.department || [] },
                    date: today
                }
            },
            {
                $facet: {
                    patients: [
                        { $unwind: '$tokens' },
                        {
                            $group: {
                                _id: null,
                                patientsServed: {
                                    $sum: {
                                        $cond: [
                                            { $eq: ['$tokens.status', 'COMPLETED'] },
                                            1,
                                            0
                                        ]
                                    }
                                },
                                patientsWaiting: {
                                    $sum: {
                                        $cond: [
                                            { $eq: ['$tokens.status', 'WAITING'] },
                                            1,
                                            0
                                        ]
                                    }
                                },
                                averageConsultationMinutes: {
                                    $avg: {
                                        $cond: [
                                            {
                                                $and: [
                                                    { $eq: ['$tokens.status', 'COMPLETED'] },
                                                    { $isNumber: '$tokens.serviceTime' },
                                                    { $gte: ['$tokens.serviceTime', 0] }
                                                ]
                                            },
                                            '$tokens.serviceTime',
                                            null
                                        ]
                                    }
                                }
                            }
                        }
                    ],
                    departments: [
                        {
                            $match: {
                                isActive: true,
                                queueStatus: 'ACTIVE'
                            }
                        },
                        { $group: { _id: '$department' } },
                        { $count: 'total' }
                    ]
                }
            }
        ]);

        const result = queues[0] || {};
        const stats = result.patients?.[0] || {};

        return res.status(200).json({
            success: true,
            date: today,
            patientsServed: stats.patientsServed || 0,
            averageConsultationMinutes: Math.round(
                (stats.averageConsultationMinutes || 0) * 10
            ) / 10,
            patientsWaiting: stats.patientsWaiting || 0,
            activeDepartments: result.departments?.[0]?.total || 0,
            totalDepartments: (doctor.department || []).length
        });

    } catch (error) {
        console.error('Overview statistics error:', error);

        return res.status(500).json({
            success: false,
            message: 'Failed to load overview statistics'
        });
    }
};


const getOverviewDepartments = async (req, res) => {
    try {
        // Get the logged-in doctor
        const doctor = await UserV2.findById(req.user.id)
            .select('hospitalId department doctorCode role')
            .lean();

        if (!doctor || doctor.role !== 'DOCTOR') {
            return res.status(403).json({
                success: false,
                message: 'Doctor access only'
            });
        }

        const assignedDepartments = doctor.department || [];

        // Get queues belonging to this doctor
        const queues = await Queue.find({
            hospitalId: doctor.hospitalId,
            doctorCode: doctor.doctorCode,
            department: { $in: assignedDepartments }
        }).lean();

        const departments = assignedDepartments.map((departmentName) => {
            // Find the queue for this department
            const queue = queues.find(
                (q) => q.department === departmentName
            );

            // Department has no queue yet
            if (!queue) {
                return {
                    departmentName,
                    queueStatus: 'NOT_STARTED',
                    waitingPatients: 0,
                    completedPatients: 0,
                    nextToken: null
                };
            }

            const tokens = queue.tokens || [];

            const waitingTokens = tokens.filter(
                (token) => token.status === 'WAITING'
            );

            const completedPatients = tokens.filter(
                (token) => token.status === 'COMPLETED'
            ).length;

            // Earliest waiting token
            const nextToken = waitingTokens.length > 0
                ? Math.min(
                    ...waitingTokens.map((token) => token.tokenNumber)
                )
                : null;

            let queueStatus = queue.queueStatus || 'ACTIVE';

            if (!queue.isActive || queueStatus === 'CLOSED') {
                queueStatus = 'CLOSED';
            }

            return {
                departmentName,
                queueStatus,
                waitingPatients: waitingTokens.length,
                completedPatients,
                nextToken
            };
        });

        return res.status(200).json({
            success: true,
            departments
        });

    } catch (error) {
        console.error('Doctor overview error:', error);

        return res.status(500).json({
            success: false,
            message: 'Failed to load department overview'
        });
    }
};




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
    const requestId = `NEXT-${Date.now()}`;

    try {
        console.log(`\n========== ${requestId} START ==========`);

        console.log(`[${requestId}] Request body:`, req.body);
        console.log(`[${requestId}] Authenticated user ID:`, req.user?.id);

        const { department } = req.body;

        // 1. Validate request
        if (
            !department ||
            typeof department !== "string" ||
            !department.trim()
        ) {
            console.warn(`[${requestId}] Invalid department`);

            return res.status(400).json({
                success: false,
                message: "A valid department is required",
                requestId
            });
        }

        const normalizedDepartment = department.trim();

        console.log(
            `[${requestId}] Requested department:`,
            normalizedDepartment
        );

        // 2. Find authenticated doctor
        console.log(`[${requestId}] Fetching doctor from database...`);

        const doctor = await UserV2.findById(req.user?.id)
            .select("role hospitalId doctorCode department isActive")
            .lean();

        if (!doctor || doctor.role !== "DOCTOR") {
            console.warn(`[${requestId}] Doctor not found or invalid role`);

            return res.status(403).json({
                success: false,
                message: "Only doctors can call the next patient",
                requestId
            });
        }

        console.log(`[${requestId}] Doctor found:`, {
            role: doctor.role,
            hospitalId: doctor.hospitalId,
            doctorCode: doctor.doctorCode,
            departments: doctor.department,
            isActive: doctor.isActive
        });

        if (doctor.isActive === false) {
            console.warn(`[${requestId}] Doctor account is inactive`);

            return res.status(403).json({
                success: false,
                message: "Your account is inactive",
                requestId
            });
        }

        if (!doctor.hospitalId || !doctor.doctorCode) {
            console.error(`[${requestId}] Missing hospitalId or doctorCode`);

            return res.status(400).json({
                success: false,
                message: "Doctor hospital or doctor code is missing",
                requestId
            });
        }

        // 3. Verify department assignment
        if (!(doctor.department || []).includes(normalizedDepartment)) {
            console.warn(
                `[${requestId}] Department is not assigned to this doctor`
            );

            return res.status(403).json({
                success: false,
                message: "You are not assigned to this department",
                requestId
            });
        }

        // 4. Get current Indian date
        const today = new Intl.DateTimeFormat("en-CA", {
            timeZone: "Asia/Kolkata",
            year: "numeric",
            month: "2-digit",
            day: "2-digit"
        }).format(new Date());

        console.log(`[${requestId}] Current queue date:`, today);

        // 5. Find the correct queue
        const queueQuery = {
            hospitalId: doctor.hospitalId,
            doctorCode: doctor.doctorCode,
            department: normalizedDepartment,
            date: today,
            isActive: true
        };

        console.log(`[${requestId}] Queue query:`, queueQuery);

        const queueDoc = await Queue.findOne(queueQuery);

        if (!queueDoc) {
            console.warn(`[${requestId}] No matching queue found`);

            return res.status(404).json({
                success: false,
                message: "Active queue session not found",
                requestId,
                searchedFor: {
                    hospitalId: doctor.hospitalId,
                    doctorCode: doctor.doctorCode,
                    department: normalizedDepartment,
                    date: today
                }
            });
        }

        console.log(`[${requestId}] Queue found:`, {
            queueId: queueDoc._id.toString(),
            queueStatus: queueDoc.queueStatus,
            isActive: queueDoc.isActive,
            date: queueDoc.date,
            tokenCount: queueDoc.tokens.length
        });

        // 6. Validate queue status
        if (queueDoc.queueStatus === "PAUSED") {
            console.warn(`[${requestId}] Queue is paused`);

            return res.status(400).json({
                success: false,
                message: "Cannot call the next patient while the queue is paused",
                requestId
            });
        }

        if (queueDoc.queueStatus !== "ACTIVE") {
            console.warn(
                `[${requestId}] Queue is not ACTIVE:`,
                queueDoc.queueStatus
            );

            return res.status(400).json({
                success: false,
                message: "This queue is not active",
                requestId
            });
        }

        // 7. Log token statuses
        console.log(
            `[${requestId}] Token statuses:`,
            queueDoc.tokens.map(token => ({
                tokenNumber: token.tokenNumber,
                status: token.status,
                tokenId: token._id.toString()
            }))
        );

        // 8. Check for an existing consultation
        const existingActive = queueDoc.tokens.find(
            token => token.status === "IN_CONSULTATION"
        );

        if (existingActive) {
            console.warn(
                `[${requestId}] Consultation already in progress:`,
                existingActive.tokenNumber
            );

            return res.status(400).json({
                success: false,
                message: "Complete the current consultation first",
                activeTokenNumber: existingActive.tokenNumber,
                requestId
            });
        }

        // 9. Find the first waiting patient
        const nextToken = queueDoc.tokens.find(
            token => token.status === "WAITING"
        );

        if (!nextToken) {
            console.warn(`[${requestId}] No waiting tokens found`);

            return res.status(404).json({
                success: false,
                message: "No waiting patients found in the queue",
                requestId
            });
        }

        console.log(`[${requestId}] Calling token:`, {
            tokenNumber: nextToken.tokenNumber,
            tokenId: nextToken._id.toString(),
            previousStatus: nextToken.status
        });

        // 10. Start consultation
        const consultationStartedAt = new Date();

        nextToken.status = "IN_CONSULTATION";
        nextToken.consultationStartedAt = consultationStartedAt;

        console.log(`[${requestId}] Saving queue update...`);

        await queueDoc.save();

        console.log(`[${requestId}] Queue saved successfully`);

        // 11. Calculate ETA
        let etaData = null;

        try {
            console.log(`[${requestId}] Calculating ETA...`);

            etaData = await calculateETA(
                queueDoc,
                nextToken.tokenNumber
            );

            console.log(`[${requestId}] ETA calculated:`, etaData);
        } catch (etaError) {
            // A failed ETA calculation should not undo a saved consultation.
            console.error(
                `[${requestId}] ETA calculation failed:`,
                etaError
            );
        }

        // 12. Emit Socket.IO event
        try {
            console.log(`[${requestId}] Emitting TOKEN_CALLED...`);

            const io = socket.getIO();

            io.to(`queue_${queueDoc._id}`).emit("TOKEN_CALLED", {
                queueId: queueDoc._id.toString(),
                tokenId: nextToken._id.toString(),
                tokenNumber: nextToken.tokenNumber,
                status: "IN_CONSULTATION",
                consultationStartedAt,
                metrics: etaData
            });

            console.log(`[${requestId}] TOKEN_CALLED emitted successfully`);
        } catch (socketError) {
            console.error(
                `[${requestId}] Socket.IO emission failed:`,
                socketError
            );
        }

        // 13. Return response
        console.log(`[${requestId}] Consultation started successfully`);
        console.log(`========== ${requestId} END ==========\n`);

        return res.status(200).json({
            success: true,
            message: "Next patient called successfully",
            requestId,
            data: {
                sessionId: queueDoc._id,
                queueStatus: queueDoc.queueStatus,
                avgServiceTime: Math.ceil(queueDoc.avgServiceTime || 5),
                calledToken: nextToken,
                metrics: etaData,
                tokens: queueDoc.tokens
            }
        });

    } catch (error) {
        console.error(`[${requestId}] ERROR:`, {
            message: error.message,
            name: error.name,
            stack: error.stack
        });

        return res.status(500).json({
            success: false,
            message: "Internal server error",
            requestId
        });
    }
};



const completeCurrent = async (req, res) => {
    try {
        const { department } = req.body;
console.log("hit")
        if (!department || typeof department !== "string") {
            return res.status(400).json({
                success: false,
                message: "Department is required"
            });
        }

        // Get the authenticated doctor from the database.
        // Do not trust doctorCode or hospitalId supplied by the client.
        const doctor = await UserV2.findById(req.user.id)
            .select(
                "role hospitalId doctorCode name department isActive"
            )
            .lean();

        if (!doctor || doctor.role !== "DOCTOR") {
            return res.status(403).json({
                success: false,
                message: "Only doctors can complete consultations"
            });
        }

        if (doctor.isActive === false) {
            return res.status(403).json({
                success: false,
                message: "Your account is inactive"
            });
        }

        const normalizedDepartment = department.trim();

        if (!normalizedDepartment ||
            !(doctor.department || []).includes(normalizedDepartment)) {
            return res.status(403).json({
                success: false,
                message: "You are not assigned to this department"
            });
        }

        // Find only this doctor's queue in their hospital.
        const queueDoc = await Queue.findOne({
            hospitalId: doctor.hospitalId,
            doctorCode: doctor.doctorCode,
            department: normalizedDepartment,
            queueStatus: { $in: ["ACTIVE", "PAUSED"] }
        });

        if (!queueDoc) {
            return res.status(404).json({
                success: false,
                message: "Queue not found"
            });
        }

        // Find the token currently in consultation.
        const activeToken = queueDoc.tokens.find(
            token => token.status === "IN_CONSULTATION"
        );

        if (!activeToken) {
            return res.status(404).json({
                success: false,
                message: "No consultation is currently in progress"
            });
        }

        const activeQueue = await UserActiveQueue.findOne({
            queueId: queueDoc._id,
            tokenId: activeToken._id
        });

        if (!activeQueue) {
            return res.status(404).json({
                success: false,
                message: "Active patient record not found"
            });
        }

        // Calculate the consultation duration.
        const completedAt = new Date();
        const consultationStartedAt =
            activeToken.consultationStartedAt || null;

        let serviceTime = null;

        if (consultationStartedAt) {
            const elapsedMinutes =
                (completedAt.getTime() -
                    new Date(consultationStartedAt).getTime()) / 60000;

            if (Number.isFinite(elapsedMinutes) && elapsedMinutes >= 0) {
                serviceTime = Number(elapsedMinutes.toFixed(2));
            }
        }

        // Update the token.
        activeToken.status = "COMPLETED";
        activeToken.consultationCompletedAt = completedAt;
        activeToken.serviceTime = serviceTime;

        // Recalculate the average from completed consultations
        // that have a valid recorded duration.
        const completedTokens = queueDoc.tokens.filter(token =>
            token.status === "COMPLETED" &&
            token.serviceTime != null &&
            Number.isFinite(Number(token.serviceTime)) &&
            Number(token.serviceTime) >= 0
        );

        if (completedTokens.length > 0) {
            const totalServiceTime = completedTokens.reduce(
                (sum, token) => sum + Number(token.serviceTime),
                0
            );

            queueDoc.avgServiceTime = Math.ceil(
                totalServiceTime / completedTokens.length
            );
        }

        await queueDoc.save();

        // Get hospital details for the history snapshot.
        const hospital = await HospitalV2.findOne({
            code: queueDoc.hospitalId
        })
            .select("name logoUrl")
            .lean();

        // Save history idempotently.
        // Requires queueId and tokenId to identify a history record.
        const history = await QueueHistory.findOneAndUpdate(
            {
                queueId: queueDoc._id,
                tokenId: activeToken._id
            },
            {
                $setOnInsert: {
                    userId: activeQueue.userId,
                    queueId: queueDoc._id,
                    tokenId: activeToken._id,
                    tokenNumber: activeToken.tokenNumber,
                    hospitalId: queueDoc.hospitalId,
                    hospitalName: hospital?.name || "",
                    hospitalLogoUrl: hospital?.logoUrl || "",
                    doctorCode: doctor.doctorCode,
                    doctorName: doctor.name || "",
                    department: queueDoc.department,
                    roomNumber: queueDoc.roomNumber || "",
                    status: "COMPLETED",
                    date: queueDoc.date,
                    feedback: activeToken.feedback || {},
                    joinedAt: activeQueue.joinedAt || null,
                    consultationStartedAt,
                    completedAt,
                    serviceTime
                }
            },
            {
                new: true,
                upsert: true,
                setDefaultsOnInsert: true
            }
        );

        // Remove the active patient record only after history is saved.
        await UserActiveQueue.deleteOne({
            _id: activeQueue._id
        });

        // Calculate the next waiting patient's ETA.
        const nextWaitingToken = queueDoc.tokens.find(
            token => token.status === "WAITING"
        );

        let nextMetrics = null;

        if (nextWaitingToken) {
            nextMetrics = await calculateETA(
                queueDoc,
                nextWaitingToken.tokenNumber
            );
        }

        // Notify clients listening to this queue.
        const io = socket.getIO();

        io.to(`queue_${queueDoc._id}`).emit("TOKEN_COMPLETED", {
            queueId: queueDoc._id.toString(),
            tokenId: activeToken._id.toString(),
            tokenNumber: activeToken.tokenNumber,
            status: "COMPLETED",
            consultationStartedAt,
            completedAt,
            serviceTime,
            avgServiceTime: queueDoc.avgServiceTime,
            nextTokenNumber: nextWaitingToken?.tokenNumber || null,
            metrics: nextMetrics
        });

        return res.status(200).json({
            success: true,
            message: "Current consultation completed successfully",
            data: {
                sessionId: queueDoc._id,
                consultationStartedAt,
                completedAt,
                queueStatus: queueDoc.queueStatus,
                avgServiceTime: queueDoc.avgServiceTime,
                serviceTime,
                completedToken: activeToken,
                historyId: history._id,
                nextMetrics,
                tokens: queueDoc.tokens
            }
        });

    } catch (error) {
        console.error("completeCurrent error:", error);

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
  getDoctorAnalytics,
  getOverviewDepartments,
  getOverviewStats,
  getConsultationActivity
};
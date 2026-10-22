const User = require('../new_models/peron_model');
const Queue = require('../new_models/new_queuev2');
const HospitalV2= require('../new_models/new_hosp_model');
const getAdminDashboardData = async (req, res) => {
    try {
        console.log("admin entered");

        // -----------------------------------------
        // 1. Get hospital CODE
        // User hospitalId stores: "A-5198"
        // -----------------------------------------

        const hospitalcode =
            req.body.hospitalId || req.user?.hospitalId;

        console.log("Hospital code:", hospitalcode);

        if (!hospitalcode && req.user?.role !== "SUPER_ADMIN") {
            return res.status(400).json({
                success: false,
                message: "Hospital ID is missing from user profile."
            });
        }


        // -----------------------------------------
        // 2. Convert hospital CODE -> Mongo _id
        // Queue hospitalId stores Mongo _id as string
        // -----------------------------------------

        let mongoHospitalId = null;

        if (hospitalcode) {
            const hospital = await HospitalV2.findOne({
                code: hospitalcode
            }).select("_id");

            if (!hospital && req.user?.role !== "SUPER_ADMIN") {
                return res.status(404).json({
                    success: false,
                    message: "Hospital not found."
                });
            }

            mongoHospitalId = hospital?._id?.toString();
        }

        console.log("Mongo Hospital ID:", mongoHospitalId);


        // -----------------------------------------
        // 3. FILTERS
        // -----------------------------------------

        // For User collection
        const userFilter = hospitalcode
            ? { hospitalId: hospitalcode }
            : {};

        // For Queue collection
      const queueFilter = hospitalcode
    ? { hospitalId: hospitalcode }
    : {};

        console.log("User filter:", userFilter);
        console.log("Queue filter:", queueFilter);


        // -----------------------------------------
        // 4. Total queues
        // -----------------------------------------

        const totalQueues =
            await Queue.countDocuments(queueFilter);


        // -----------------------------------------
        // 5. Active queues
        // -----------------------------------------
const activeQueues = await Queue.countDocuments({
    ...queueFilter,
    queueStatus: {
        $regex: /^(ACTIVE|PAUSED)$/i
    }
});

        // -----------------------------------------
        // 6. Total doctors
        // IMPORTANT:
        // User uses hospital CODE
        // -----------------------------------------

        const totalDoctors =
            await User.countDocuments({
                ...userFilter,
                role: "DOCTOR"
            });


        // -----------------------------------------
        // 7. Doctors having active/paused queues
        // IMPORTANT:
        // Queue uses Mongo hospital ID
        // -----------------------------------------

      const activeQueueDocs = await Queue.find({
    ...queueFilter,
    queueStatus: {
        $regex: /^(ACTIVE|PAUSED)$/i
    }
}).distinct("doctorCode");

const doctorsWithQueues = activeQueueDocs.length;
      


        // -----------------------------------------
        // 8. Total waiting patients
        // -----------------------------------------

        const allQueues =
            await Queue.find(queueFilter);

        let totalWaitingPatients = 0;

        allQueues.forEach(q => {

            if (
                q.tokens &&
                Array.isArray(q.tokens)
            ) {

                totalWaitingPatients +=
                    q.tokens.filter(t =>
                        t.status === "WAITING" ||
                        t.status === "waiting"
                    ).length;
            }
        });


        // -----------------------------------------
        // 9. Response
        // -----------------------------------------

        return res.status(200).json({
            success: true,
            message: "Dashboard metrics calculated successfully",

            data: {
                totalQueues,
                activeQueues,
                totalDoctors,
                doctorsWithQueues,
                totalWaitingPatients
            }
        });

    } catch (error) {

        console.log(
            "Dashboard Error:",
            error.message
        );

        return res.status(500).json({
            success: false,
            error: error.message
        });
    }
};


const getGlobalQueues = async (req, res) => {
    const DEBUG = "[GLOBAL_QUEUES]";

    try {
        console.log(`\n${DEBUG} ===== REQUEST START =====`);
        console.log(`${DEBUG} User:`, {
            id: req.user?.id,
            role: req.user?.role,
            hospitalId: req.user?.hospitalId
        });
        console.log(`${DEBUG} Request body:`, req.body);

        // 1. Resolve the hospital code
        const hospitalCode =
            req.user?.role === "SUPER_ADMIN"
                ? (req.body?.hospitalId || req.query?.hospitalId)
                : (req.user?.hospitalId || req.body?.hospitalId);

        console.log(`${DEBUG} Resolved hospitalCode:`, hospitalCode);

        if (!hospitalCode) {
            console.error(`${DEBUG} Hospital ID missing`);

            return res.status(400).json({
                success: false,
                message: "Hospital ID is missing."
            });
        }

        // 2. Fetch only queues that are currently active
        const queueFilter = {
            hospitalId: hospitalCode,
            isActive: true,
            queueStatus: "ACTIVE",
            doctorCode: {
                $exists: true,
                $nin: ["", null]
            }
        };

        console.log(`${DEBUG} Queue filter:`, queueFilter);

        const queues = await Queue.find(queueFilter)
            .sort({ updatedAt: -1 })
            .lean();

        console.log(`${DEBUG} Active queues found: ${queues.length}`);

        queues.forEach((queue, index) => {
            const tokens = Array.isArray(queue.tokens)
                ? queue.tokens
                : [];

            console.log(`${DEBUG} Queue ${index + 1}:`, {
                queueId: queue._id?.toString(),
                hospitalId: queue.hospitalId,
                department: queue.department,
                doctorCode: queue.doctorCode,
                date: queue.date,
                isActive: queue.isActive,
                queueStatus: queue.queueStatus,
                waitingCount: tokens.filter(token =>
                    ["WAITING", "waiting"].includes(token.status)
                ).length
            });
        });

        // 3. Group active queues by department
        const departments = new Map();

        for (const queue of queues) {
            const departmentName =
                (queue.department || queue.queueName || "").trim();

            if (!departmentName || !queue.doctorCode) {
                console.warn(`${DEBUG} Skipping invalid queue`, {
                    queueId: queue._id?.toString(),
                    departmentName,
                    doctorCode: queue.doctorCode
                });
                continue;
            }

            const departmentKey = departmentName.toLowerCase();

            if (!departments.has(departmentKey)) {
                departments.set(departmentKey, {
                    departmentName,
                    doctors: new Map(),
                    waitingCount: 0,
                    queueIds: [],
                    avgServiceTimes: []
                });
            }

            const department = departments.get(departmentKey);
            const doctorCode = queue.doctorCode;

            // A doctor counts once per department.
            // Queues are sorted newest first, so the first one wins.
            if (department.doctors.has(doctorCode)) {
                console.warn(`${DEBUG} Duplicate doctor queue ignored:`, {
                    department: departmentName,
                    doctorCode,
                    ignoredQueueId: queue._id?.toString(),
                    keptQueueId:
                        department.doctors.get(doctorCode).queueId
                });
                continue;
            }

            const tokens = Array.isArray(queue.tokens)
                ? queue.tokens
                : [];

            const waitingCount = tokens.filter(token =>
                ["WAITING", "waiting"].includes(token.status)
            ).length;

            department.doctors.set(doctorCode, {
                doctorCode,
                queueId: queue._id.toString()
            });

            department.waitingCount += waitingCount;
            department.queueIds.push(queue._id.toString());

            const serviceTime = Number(queue.avgServiceTime);
            if (Number.isFinite(serviceTime) && serviceTime > 0) {
                department.avgServiceTimes.push(serviceTime);
            }
        }

        // 4. Build one response card per department
        const data = Array.from(departments.values()).map(department => {
            const waitingCount = department.waitingCount;

            const serviceTimes = department.avgServiceTimes;
            const avgServiceTime = serviceTimes.length
                ? serviceTimes.reduce((sum, value) => sum + value, 0)
                    / serviceTimes.length
                : 5;

            const totalDoctors = department.doctors.size;

            const loadStatus =
                waitingCount >= 15
                    ? "HIGH_LOAD"
                    : waitingCount >= 8
                        ? "MODERATE"
                        : "NORMAL";

            const result = {
                // Stable UI grouping ID, not a MongoDB queue ID.
                departmentId: department.departmentName
                    .trim()
                    .toLowerCase()
                    .replace(/\s+/g, "-"),

                departmentName: department.departmentName,

                // Android adapter displays this field.
                assignedDoctorsCount: totalDoctors,

                location: "Hospital",
                waitingCount,
                avgServiceTime,
                avgWaitTime:
                    `${Math.ceil(waitingCount * avgServiceTime)}m`,
                loadStatus,
                queueStatus: "ACTIVE",

                doctorCodes: Array.from(department.doctors.keys()),
                queueIds: department.queueIds
            };

            console.log(`${DEBUG} Department summary:`, result);

            return result;
        });

        console.log(`${DEBUG} Final department count: ${data.length}`);
        console.log(
            `${DEBUG} Final response:`,
            JSON.stringify(data, null, 2)
        );
        console.log(`${DEBUG} ===== REQUEST END =====\n`);

        return res.status(200).json({
            success: true,
            message: "Global queues fetched successfully",
            data
        });

    } catch (error) {
        console.error(`${DEBUG} ERROR:`, error);
        console.error(`${DEBUG} Stack:`, error.stack);

        return res.status(500).json({
            success: false,
            message: "Failed to fetch global queues",
            error: error.message
        });
    }
};

const getDoctorDirectory = async (req, res) => {
    try {

        // 1. Get manual hospital code
        const hospitalcode =
            req.body.hospitalId || req.user?.hospitalId;

        console.log("Hospital code:", hospitalcode);

        if (!hospitalcode && req.user?.role !== "SUPER_ADMIN") {
            return res.status(400).json({
                success: false,
                message: "Hospital ID is missing."
            });
        }
        const hospital = await HospitalV2.findOne({
    code: hospitalcode
}).select("_id");

        // 2. Convert manual hospital code → MongoDB _id
        
       const hospitalId = hospital?._id?.toString();

console.log("Mongo Hospital ID:", hospitalId);


// -----------------------------------------
// 3. Get doctors
// User.hospitalId stores hospital CODE
// Example: "A-5198"
// -----------------------------------------

const doctors = await User.find({
    hospitalId: hospitalcode,
    role: "DOCTOR"
}).lean();

console.log("Doctors found:", doctors.length);


// -----------------------------------------
// 4. Get doctor codes
// -----------------------------------------

const doctorCodes = doctors
    .map(d => d.doctorCode)
    .filter(Boolean);

console.log("Doctor codes:", doctorCodes);


// -----------------------------------------
// 5. Get queues
// Queue.hospitalId stores Mongo hospital ID
// Example: "6a7edcd1552bf90070deabd8"
// -----------------------------------------
// -----------------------------------------
// 5. Get queues
// Queue.hospitalId stores hospital CODE
// Example: "A-5198"
// -----------------------------------------

const queues = await Queue.find({
    hospitalId: hospitalcode,
    doctorCode: { $in: doctorCodes }
})
.sort({ updatedAt: -1 })
.lean();

console.log("Queues found:", queues.length);
        // -----------------------------------------
        // 7. Latest queue for each doctor
        // -----------------------------------------

        const latestQueueByDoctor = new Map();
console.log(queues.length)
for (const queue of queues) {
console.log("entered")
            if (!latestQueueByDoctor.has(queue.doctorCode)) {

                latestQueueByDoctor.set(
                    queue.doctorCode,
                    queue
                );
            }
        }

        // -----------------------------------------
        // 8. Combine doctor + queue
        // -----------------------------------------

        const directoryData = doctors.map(doc => {

            const queue =
                latestQueueByDoctor.get(doc.doctorCode);

            let status = "OFF_DUTY";

            if (queue) {

                if (queue.queueStatus === "ACTIVE") {
                    status = "ACTIVE";
                }

                else if (queue.queueStatus === "PAUSED") {
                    status = "PAUSED";
                }

                else if (queue.queueStatus === "CLOSED") {
                    status = "CLOSED";
                }
            }

            const formattedTokens =
                queue && Array.isArray(queue.tokens)
                    ? queue.tokens.map(t => ({
                        tokenId: t._id || t.tokenId,
                        tokenNumber: t.tokenNumber,
                        userId: t.userId,
                        patientName: t.patientName,
                        status: t.status,
                        amountPaid: t.amountPaid
                    }))
                    : [];

            return {
                _id: doc._id,
                name: doc.name,
                email: doc.email,

                specialization:
                    Array.isArray(doc.department)
                        ? doc.department[0]
                        : (doc.department || "General Practice"),

                doctorCode: doc.doctorCode,
                role: doc.role,
                hospitalId: doc.hospitalId,

                status: status,

                activeQueue: queue
                    ? {
                        _id: queue._id,
                        hospitalId: queue.hospitalId,
                        doctorCode: queue.doctorCode,
                        queueStatus: queue.queueStatus,
                        tokens: formattedTokens
                    }
                    : null
            };
        });

        return res.status(200).json({
            success: true,
            message: "Doctor directory fetched successfully",
            data: directoryData
        });

    } catch (error) {

        console.log(
            "Error fetching doctor directory:",
            error.message
        );

        return res.status(500).json({
            success: false,
            error: error.message
        });
    }
};

module.exports = {
  getAdminDashboardData,
  getDoctorDirectory,
   getGlobalQueues
}
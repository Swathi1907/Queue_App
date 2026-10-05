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
    try {

        // -----------------------------------------
        // 1. Get hospital CODE
        // -----------------------------------------

        const hospitalcode =
            req.body.hospitalId || req.user?.hospitalId;

        console.log("Hospital code:", hospitalcode);

        if (!hospitalcode && req.user?.role !== "SUPER_ADMIN") {
            return res.status(400).json({
                success: false,
                message: "Hospital ID is missing."
            });
        }


        // -----------------------------------------
        // 2. Convert hospital CODE -> MongoDB _id
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
        // 3. Queue filter
        // Queue.hospitalId stores Mongo ID as STRING
        // -----------------------------------------

        const filter = mongoHospitalId
            ? { hospitalId: hospitalcode }
            : {};

        console.log("Queue filter:", filter);


        // -----------------------------------------
        // 4. Get ALL queues
        // -----------------------------------------

        const queues = await Queue.find(filter)
            .sort({ updatedAt: -1 })
            .lean();

        console.log("Total queues found:", queues.length);


        // -----------------------------------------
        // 5. Keep ONLY latest queue for each
        //    department
        // -----------------------------------------

        const latestQueueByDepartment = new Map();

        for (const queue of queues) {

            const departmentName =
                queue.department || queue.queueName;

            if (!departmentName) {
                continue;
            }

            // Queues are sorted newest first,
            // so first queue = latest queue
            if (!latestQueueByDepartment.has(departmentName)) {

                latestQueueByDepartment.set(
                    departmentName,
                    queue
                );
            }
        }


        // -----------------------------------------
        // 6. Build global queue cards
        // -----------------------------------------

        const globalQueueItems =
            await Promise.all(

                Array.from(
                    latestQueueByDepartment.values()
                ).map(async (queue) => {

                    const tokens =
                        Array.isArray(queue.tokens)
                            ? queue.tokens
                            : [];


                    // -----------------------------------------
                    // Waiting patients
                    // -----------------------------------------

                    const waitingCount =
                        tokens.filter(t =>
                            t.status === "WAITING" ||
                            t.status === "waiting"
                        ).length;


                    // -----------------------------------------
                    // Load status
                    // -----------------------------------------

                    let loadStatus = "NORMAL";

                    if (waitingCount >= 15) {
                        loadStatus = "HIGH_LOAD";
                    }
                    else if (waitingCount >= 8) {
                        loadStatus = "MODERATE";
                    }


                    // -----------------------------------------
                    // Average service time
                    // -----------------------------------------

                    const avgServiceTimeMinutes =
                        queue.avgServiceTime || 5;


                    // -----------------------------------------
                    // Count active doctors
                    // -----------------------------------------

                    const departmentName =
                        queue.department ||
                        queue.queueName;

                    const activeDoctorsCount =
                        await Queue.countDocuments({

                            ...filter,

                            department: departmentName,

                            queueStatus: {
                                $in: [
                                    "ACTIVE",
                                    "PAUSED",
                                    "active",
                                    "paused"
                                ]
                            },

                            doctorCode: {
                                $exists: true,
                                $ne: null
                            }
                        });


                    // -----------------------------------------
                    // Return queue card
                    // -----------------------------------------

                    return {

                        departmentId:
                            queue._id.toString(),

                        departmentName:
                            departmentName ||
                            `Dr. ${queue.doctorCode} Queue`,

                        location:
                            queue.location ||
                            "Main Building, Floor 2",

                        waitingCount:
                            waitingCount,

                        avgWaitTime:
                            queue.avgWaitTime ||
                            `${Math.max(
                                5,
                                waitingCount *
                                avgServiceTimeMinutes
                            )}m`,

                        avgServiceTime:
                            `${avgServiceTimeMinutes} mins/patient`,

                        loadStatus:
                            loadStatus,

                        assignedDoctorsCount:
                            activeDoctorsCount,

                        queueStatus:
                            queue.queueStatus
                    };
                })
            );


        // -----------------------------------------
        // 7. Response
        // -----------------------------------------

        return res.status(200).json({

            success: true,

            message:
                "Global queues fetched successfully",

            data:
                globalQueueItems
        });


    } catch (error) {

        console.log(
            "Error fetching global queues:",
            error.message
        );

        return res.status(500).json({

            success: false,

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
};
const QueueMember = require("../models/queue_member.models");
const Doctor = require("../models/doctor_model");
const Hospital = require("../models/hospital.models");

async function buildDoctorContext(userId, filters = {}) {

    const members = await QueueMember.find({
        userId,
        status: { $in: ["waiting", "serving"] }
    }).populate("queueId");

    if (members.length === 0) {
        return null;
    }

    let doctorContext = "";

    for (const member of members) {

        const queue = member.queueId;

        const hospital = await Hospital.findOne({
            hospitalId: queue.hospitalId
        });

        let query = {
            hospitalcode: queue.hospitalId
        };

        if (filters.doctorName) {
            query.doctorName = {
                $regex: filters.doctorName,
                $options: "i"
            };
        }

        if (filters.specialization) {
            query.specialization = {
                $regex: filters.specialization,
                $options: "i"
            };
        }

        if (filters.availableDay) {
            query.availableDays = filters.availableDay;
        }

        const doctors = await Doctor.find(query);

        doctorContext += `
--------------------------------

Hospital: ${hospital.hospitalName}

Department: ${queue.queueName}

`;

        if (doctors.length === 0) {

            doctorContext += "No matching doctors found.\n";

        } else {

            doctors.forEach(doc => {

                doctorContext += `
Doctor: ${doc.doctorName}

Specialization: ${doc.specialization}

Qualification: ${doc.qualification}

Room: ${doc.roomNumber}

Available Days: ${doc.availableDays.join(", ")}

Consultation Time: ${doc.startTime} - ${doc.endTime}

--------------------------
`;

            });

        }
    }

    return doctorContext;
}

module.exports = {
    buildDoctorContext
}; 
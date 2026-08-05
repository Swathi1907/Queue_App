const UserV2 = require('../new_models/peron_model');
const HospitalV2 = require('../new_models/new_hosp_model');


// 1. Fetch Assigned Doctor Profile and Department Details
const getDoctorProfile = async (req, res) => {
  try {
    // req.user comes from the auth middleware decoding the JWT token
    const doctorId = req.user.id;

    const doctor = await UserV2.findById(doctorId).select('-password');

    if (!doctor) {
      return res.status(404).json({
        success: false,
        message: "Doctor profile not found.",
      });
    }

    // Ensure department is always returned as an array
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
        department: departmentList, // Now strictly a list/array
        hospitalId: doctor.hospitalId,
        rating:doctor.rating,
        phoneNumber:doctor.phoneNumber,
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

const Queue = require('../new_models/new_queuev2');
const session_there = async (req, res) => {
    try {
        console.log("Session_there hit")
        const { department, doctorCode } = req.query;
        const todayDate = new Date().toISOString().split('T')[0];

        if (!doctorCode || !department) {
            return res.status(400).json({
                success: false,
                message: "doctorCode and department parameters are required"
            });
        }

        // Find today's queue document based on queueStatus instead of isActive
        const queueDoc = await Queue.findOne({
            doctorCode: doctorCode,
            department: department,
            queueStatus: { $ne: 'CLOSED' } // Ensures we fetch queues that are ACTIVE or PAUSED, but not closed
        });

        if (!queueDoc) {
            return res.status(200).json({
                success: true,
                message: "No active queue found for today",
                data: null
            });
        }

        // Return the full session structure containing queue metadata and the tokens array
        return res.status(200).json({
            success: true,
            message: "Active session retrieved successfully",
            data: {
                sessionId: queueDoc._id,
                queueStatus: queueDoc.queueStatus, // 'ACTIVE', 'PAUSED'
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

 // Point to your queueV2 model file path

const next =  async (req, res) => {
    try {
        const { department, doctorCode} = req.body; // Pass 'date' (YYYY-MM-DD) or use today's date

        if (!doctorCode || !department) {
            return res.status(400).json({
                success: false,
                message: "doctorCode and department parameters are required"
            });
        }

        // Find the active queue for the doctor, department, and date
        const query = { doctorCode, department, queueStatus: { $ne: 'CLOSED' } };
   //     if (date) query.date = date;

        const queueDoc = await Queue.findOne(query);

        if (!queueDoc) {
            return res.status(404).json({ success: false, message: "Active queue session not found" });
        }

        // Optional check: ensure there isn't already someone active
        const existingActive = queueDoc.tokens.find(t => t.status === 'IN_CONSULTATION');
        if (existingActive) {
            return res.status(400).json({
                success: false,
                message: "A patient is already in consultation. Complete the current consultation first."
            });
        }

        // Find the next WAITING patient and make them IN_CONSULTATION
        const nextToken = queueDoc.tokens.find(t => t.status === 'WAITING');
        
        if (!nextToken) {
            return res.status(404).json({
                success: false,
                message: "No waiting patients found in the queue"
            });
        }

        nextToken.status = 'IN_CONSULTATION';
        await queueDoc.save();

        return res.status(200).json({
            success: true,
            message: "Next patient called successfully",
            data: {
                sessionId: queueDoc._id,
                queueStatus: queueDoc.queueStatus,
                tokens: queueDoc.tokens
            }
        });

    } catch (error) {
        console.error("Error calling next patient:", error);
        return res.status(500).json({ success: false, message: "Internal server error" });
    }
}

const completeCurrent = async (req, res) => {
    try {
        const { department, doctorCode } = req.body;

        if (!doctorCode || !department) {
            return res.status(400).json({
                success: false,
                message: "doctorCode and department parameters are required"
            });
        }

        const query = { doctorCode, department, queueStatus: { $ne: 'CLOSED' } };
      //  if (date) query.date = date;

        const queueDoc = await Queue.findOne(query);

        if (!queueDoc) {
            return res.status(404).json({ success: false, message: "Active queue session not found" });
        }

        // Find the patient currently in consultation
        const activeToken = queueDoc.tokens.find(t => t.status === 'IN_CONSULTATION');
        
        if (!activeToken) {
            return res.status(404).json({ 
                success: false, 
                message: "No patient currently in consultation to complete" 
            });
        }

        // Update status to COMPLETED
        activeToken.status = 'COMPLETED';
        await queueDoc.save();

        return res.status(200).json({
            success: true,
            message: "Current consultation completed successfully",
            data: {
                sessionId: queueDoc._id,
                queueStatus: queueDoc.queueStatus,
                tokens: queueDoc.tokens
            }
        });

    } catch (error) {
        console.error("Error completing current consultation:", error);
        return res.status(500).json({ success: false, message: "Internal server error" });
    }
};

module.exports = {
  getDoctorProfile,
  session_there,
  next,
  completeCurrent
};
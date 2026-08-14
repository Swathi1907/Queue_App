const UserV2 = require('../new_models/peron_model');
const HospitalV2 = require('../new_models/new_hosp_model');
const Queue = require("../new_models/new_queuev2");
const { calculateETA } = require("../new_controllers/eta_control");

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
        // Optional tracking timestamp for precise remaining duration calculations in ETA
        nextToken.createdAt = new Date(); 
        await queueDoc.save();

        // Calculate dynamic ETA metrics for the newly called token
        const etaData = await calculateETA(queueDoc, nextToken.tokenNumber);

        return res.status(200).json({
            success: true,
            message: "Next patient called successfully",
            data: {
                sessionId: queueDoc._id,
                queueStatus: queueDoc.queueStatus,
                avgServiceTime: queueDoc.avgServiceTime || 5,
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

        const query = { doctorCode, department, queueStatus: { $ne: 'CLOSED' } };
        const queueDoc = await Queue.findOne(query);

        if (!queueDoc) {
            return res.status(404).json({ success: false, message: "Active queue session not found" });
        }

        const activeToken = queueDoc.tokens.find(t => t.status === 'IN_CONSULTATION');
        
        if (!activeToken) {
            return res.status(404).json({ 
                success: false, 
                message: "No patient currently in consultation to complete" 
            });
        }

        activeToken.status = 'COMPLETED';
        await queueDoc.save();

        // Optionally calculate metrics for the next person in line to keep dashboard live
        const nextWaitingToken = queueDoc.tokens.find(t => t.status === 'WAITING');
        let nextMetrics = null;
        if (nextWaitingToken) {
            nextMetrics = await calculateETA(queueDoc, nextWaitingToken.tokenNumber);
        }

        return res.status(200).json({
            success: true,
            message: "Current consultation completed successfully",
            data: {
                sessionId: queueDoc._id,
                queueStatus: queueDoc.queueStatus,
                avgServiceTime: queueDoc.avgServiceTime || 5,
                completedToken: activeToken,
                nextMetrics: nextMetrics,
                tokens: queueDoc.tokens
            }
        });

    } catch (error) {
        console.error("Error completing current consultation:", error);
        return res.status(500).json({ success: false, message: "Internal server error" });
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
};

module.exports = {
  getDoctorProfile,
  session_there,
  next,
  completeCurrent,
  updateQueueStatus
};
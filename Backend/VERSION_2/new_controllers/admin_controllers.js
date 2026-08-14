const User = require('../new_models/peron_model');
const Queue = require('../new_models/new_queueV2');

const getAdminDashboardData = async (req, res) => {
  try {
    console.log("admin entered");
    
    // 1. Safely extract raw hospitalId, checking if it was sent nested inside an object
    let rawHospitalId = req.body.hospitalId || req.user?.hospitalId;
    
    if (rawHospitalId && typeof rawHospitalId === 'object') {
      // Handles cases where req.body.hospitalId itself became an object
      rawHospitalId = rawHospitalId.hospitalId || Object.values(rawHospitalId)[0];
    }
    
    const hospitalId = typeof rawHospitalId === 'string' ? rawHospitalId.trim() : rawHospitalId;

    if (!hospitalId && req.user?.role !== 'SUPER_ADMIN') {
      return res.status(400).json({
        success: false,
        message: 'Hospital ID is missing from user profile.'
      });
    }

    // 2. Build explicit, flat string filters
    const filter = hospitalId ? { hospitalId: hospitalId } : {};
    console.log("Cleaned Database Filter:", filter);

    // 3. Total queues in the hospital database
    const totalQueues = await Queue.countDocuments(filter);

    // 4. Active queues (queueStatus is ACTIVE or PAUSED)
    const activeQueues = await Queue.countDocuments({ 
      ...filter, 
      queueStatus: { $in: ['ACTIVE', 'PAUSED', 'active', 'paused'] } 
    });

    // 5. Total doctors linked to the hospital
    const totalDoctors = await User.countDocuments({ ...filter, role: 'DOCTOR' });

    // 6. Doctors with active or paused queues (distinct doctorCodes)
    const activeQueueDocs = await Queue.find({ 
      ...filter, 
      queueStatus: { $in: ['ACTIVE', 'PAUSED', 'active', 'paused'] } 
    }).distinct('doctorCode');
    
    const doctorsWithQueues = activeQueueDocs.length;

    // 7. Total waiting patients across all queues in the tokens array
    const allQueues = await Queue.find(filter);
    let totalWaitingPatients = 0;
    
    allQueues.forEach(q => {
      if (q.tokens && Array.isArray(q.tokens)) {
        totalWaitingPatients += q.tokens.filter(t => t.status === 'WAITING' || t.status === 'waiting').length;
      }
    });

    return res.status(200).json({
      success: true,
      message: 'Dashboard metrics calculated successfully',
      data: {
        totalQueues,
        activeQueues,
        totalDoctors,
        doctorsWithQueues,
        totalWaitingPatients
      }
    });

  } catch (error) {
    console.log("Dashboard Error:", error.message);
    return res.status(500).json({ success: false, error: error.message });
  }
};


const getGlobalQueues = async (req, res) => {
  try {
    let hospitalId = req.body.hospitalId || req.user?.hospitalId;
    if (typeof hospitalId === 'string') {
      hospitalId = hospitalId.trim();
    }

    if (!hospitalId && req.user?.role !== 'SUPER_ADMIN') {
      return res.status(400).json({
        success: false,
        message: 'Hospital ID is missing.'
      });
    }

    const filter = hospitalId ? { hospitalId } : {};

    // Fetch all queues for the hospital
    const queues = await Queue.find(filter).lean();

    // Map each queue entry into the global monitor structure expected by Android
    const globalQueueItems = await Promise.all(queues.map(async (queue) => {
      const tokens = Array.isArray(queue.tokens) ? queue.tokens : [];
      
      // Count waiting patients
      const waitingCount = tokens.filter(t => 
        t.status === 'WAITING' || t.status === 'waiting'
      ).length;

      // Calculate or assign dynamic load status based on waiting count
      let loadStatus = 'NORMAL';
      if (waitingCount >= 15) {
        loadStatus = 'HIGH_LOAD';
      } else if (waitingCount >= 8) {
        loadStatus = 'MODERATE';
      }

      const avgServiceTimeMinutes = queue.avgServiceTime || 5;

      // Count active or paused doctor queues linked to this specific department
      const departmentName = queue.department || queue.queueName;
      const activeDoctorsCount = await Queue.countDocuments({
        ...filter,
        ...(departmentName ? { department: departmentName } : {}),
        queueStatus: { $in: ['ACTIVE', 'PAUSED', 'active', 'paused'] },
        doctorCode: { $exists: true, $ne: null }
      });

      return {
        departmentId: queue._id.toString(),
        departmentName: departmentName || `Dr. ${queue.doctorCode} Queue`,
        location: queue.location || 'Main Building, Floor 2',
        waitingCount: waitingCount,
        avgWaitTime: queue.avgWaitTime || `${Math.max(5, waitingCount * avgServiceTimeMinutes)}m`,
        avgServiceTime: `${avgServiceTimeMinutes} mins/patient`,
        loadStatus: loadStatus,
        assignedDoctorsCount: activeDoctorsCount // Updated with active/paused count for this department
      };
    }));

    return res.status(200).json({
      success: true,
      message: 'Global queues fetched successfully',
      data: globalQueueItems
    });

  } catch (error) {
    console.log("Error fetching global queues:", error.message);
    return res.status(500).json({ success: false, error: error.message });
  }
};

const getDoctorDirectory = async (req, res) => {
  try {
    let hospitalId = req.body.hospitalId || req.user?.hospitalId;
    if (typeof hospitalId === 'string') {
      hospitalId = hospitalId.trim();
    }

    if (!hospitalId && req.user?.role !== 'SUPER_ADMIN') {
      return res.status(400).json({
        success: false,
        message: 'Hospital ID is missing.'
      });
    }

    const filter = hospitalId ? { hospitalId } : {};

    // 1. Fetch all doctors linked to this hospital
    const doctors = await User.find({ ...filter, role: 'DOCTOR' }).lean();

    // 2. Fetch all queues for these doctors
    const doctorCodes = doctors.map(d => d.doctorCode).filter(Boolean);
    const activeQueues = await Queue.find({
      ...filter,
      doctorCode: { $in: doctorCodes }
    }).lean();

    // 3. Combine doctor info with their queue and tokens
    const directoryData = doctors.map(doc => {
      const queue = activeQueues.find(q => q.doctorCode === doc.doctorCode);

      // Status evaluated strictly from queue state
      let status = "OFF_DUTY";
      if (queue) {
        if (queue.queueStatus === 'ACTIVE') {
          status = "ACTIVE";
        } else if (queue.queueStatus === 'PAUSED') {
          status = "PAUSED";
        }
      }

      const formattedTokens = queue && Array.isArray(queue.tokens) 
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
        specialization: Array.isArray(doc.department) ? doc.department[0] : (doc.department || "General Practice"),
        doctorCode: doc.doctorCode,
        role: doc.role,
        hospitalId: doc.hospitalId,
        status: status,
        activeQueue: queue ? {
          _id: queue._id,
          hospitalId: queue.hospitalId,
          doctorCode: queue.doctorCode,
          queueStatus: queue.queueStatus,
          tokens: formattedTokens
        } : null
      };
    });

    return res.status(200).json({
      success: true,
      message: 'Doctor directory fetched successfully',
      data: directoryData
    });

  } catch (error) {
    console.log("Error fetching doctor directory:", error.message);
    return res.status(500).json({ success: false, error: error.message });
  }
};


module.exports = {
  getAdminDashboardData,
  getDoctorDirectory,
   getGlobalQueues
};
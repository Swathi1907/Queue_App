const QueueV2 = require('../new_models/new_queuev2');
const UserV2 = require('../new_models/peron_model');
const HospitalV2 = require('../new_models/new_hosp_model');
const createDepartmentQueue = async (req, res) => {
  try {
    const { hospitalId, department, doctorCode, queueStatus } = req.body;
    const today = new Date().toISOString().split('T')[0];

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

    // 2. Check if a queue already exists for this doctor today
    const existingQueue = await QueueV2.findOne({ doctorCode, date: today });
    if (existingQueue) {
      return res.status(400).json({ 
        success: false, 
        message: 'Queue for this doctor already exists for today.' 
      });
    }

    // 3. Create the queue with the provided or default queueStatus
    const newQueue = await QueueV2.create({
      hospitalId,
      department: doctor.department[0],
      doctorCode,
      date: today,
      queueStatus: queueStatus || 'ACTIVE', // Defaults to ACTIVE if omitted
      tokens: []
    });

    return res.status(201).json({
      success: true,
      message: 'Department queue created successfully.',
      data: newQueue
    });

  } catch (error) {
    return res.status(500).json({ success: false, error: error.message });
  }
};

/*
const getUserQueuesDashboard = async (req, res) => {
  try {
    const { userId } = req.params;
    const today = new Date().toISOString().split('T')[0];

    // 1. Fetch ALL queues where the user has a token (past or present)
    const allUserQueues = await QueueV2.find({
      "tokens.userId": userId
    })
    .sort({ createdAt: -1 })
    .lean();

    let formattedActiveQueue = null;
    const historyList = [];

    // 2. Sort them on the fly into Active (today) vs History (past or finished)
    for (const queue of allUserQueues) {
      const userToken = queue.tokens.find(t => t.userId.toString() === userId);
      if (!userToken) continue;

      // Check if this queue is for today and still ongoing/paused/waiting
      if (queue.date === today && (userToken.status === "WAITING" || userToken.status === "PAUSED" || queue.isActive)) {
        if (!formattedActiveQueue) { // Grab the first matching active one for today
          const hospital = await HospitalV2.findOne({ 
            $or: [{ code: queue.hospitalId }, { _id: queue.hospitalId.match(/^[0-9a-fA-F]{24}$/) ? queue.hospitalId : null }] 
          }).lean();

          const waitingTokens = queue.tokens.filter(t => t.status === "WAITING");
          const userIndex = waitingTokens.findIndex(t => t.userId.toString() === userId);
          const peopleAhead = userIndex > 0 ? userIndex : 0;
          const estWaitTime = peopleAhead * 10;

          formattedActiveQueue = {
            queueId: queue._id,
            hospitalName: hospital ? hospital.name : "City Central Hospital",
            hospitalLogoUrl: hospital ? hospital.logoUrl : "",
            doctorDetails: `${queue.department} • ${queue.doctorCode}`,
            status: userToken.status,
            peopleAheadText: `${peopleAhead} people ahead`,
            estWaitTimeText: `${estWaitTime} min`,
            tokenNumber: userToken.tokenNumber
          };
          continue;
        }
      }

      // Everything else goes into recent history
      historyList.push({
        queueId: queue._id,
        hospitalName: "City Central Hospital", // Can map hospital lookup here if needed
        subText: `${queue.department} • ${queue.date}`,
        date: queue.date
      });
    }

    return res.status(200).json({
      success: true,
      data: {
        activeQueue: formattedActiveQueue, // Null if none active today, otherwise displays active/paused state
        recentHistory: historyList.slice(0, 5) // Limit to last 5 history items
      }
    });

  } catch (error) {
    return res.status(500).json({ success: false, error: error.message });
  }
};


*/

const getUserQueuesDashboard = async (req, res) => {
  try {
    const { userId } = req.params;
    const today = new Date().toISOString().split('T')[0];

    // 1. Fetch ALL queues where the user has a token
    const allUserQueues = await QueueV2.find({
      "tokens.userId": userId
    })
    .sort({ createdAt: -1 })
    .lean();

    const activeQueueList = [];
    const historyList = [];

    // 2. Loop through all queues and split them appropriately
    for (const queue of allUserQueues) {
      const userToken = queue.tokens.find(t => t.userId.toString() === userId);
      if (!userToken) continue;

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

      // Check if this queue is active for today
      const isActiveToday = queue.date === today && (userToken.status === "WAITING" || userToken.status === "PAUSED" || queue.isActive);

      if (isActiveToday) {
        const waitingTokens = queue.tokens.filter(t => t.status === "WAITING");
        const userIndex = waitingTokens.findIndex(t => t.userId.toString() === userId);
        const peopleAhead = userIndex > 0 ? userIndex : 0;
        const estWaitTime = peopleAhead * 10;

        activeQueueList.push({
          queueId: queue._id,
          hospitalName: hospital ? hospital.name : "City Central Hospital",
          hospitalLogoUrl: hospital ? hospital.logoUrl : "",
          doctorDetails: `${queue.department} • ${doctorDisplayName}`,
          status: userToken.status,
          peopleAheadText: `${peopleAhead} people ahead`,
          estWaitTimeText: `${estWaitTime} min`,
          tokenNumber: userToken.tokenNumber,
          date: queue.date
        });
      } else {
        // Push finished or past queues into history
        historyList.push({
          queueId: queue._id,
          hospitalName: hospital ? hospital.name : "City Central Hospital",
          subText: `${queue.department} • ${doctorDisplayName} (${queue.date})`,
          date: queue.date,
          status: userToken.status
        });
      }
    }

    return res.status(200).json({
      success: true,
      data: {
        activeQueue: activeQueueList,
        recentHistory: historyList.slice(0, 5)
      }
    });

  } catch (error) {
    return res.status(500).json({ success: false, error: error.message });
  }
};

module.exports={
    createDepartmentQueue,
    getUserQueuesDashboard,
}
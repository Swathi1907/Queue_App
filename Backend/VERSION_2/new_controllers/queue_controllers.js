
const QueueV2 = require('../new_models/new_queuev2');
const UserV2 = require('../new_models/peron_model');

const createDepartmentQueue = async (req, res) => {
  try {
    const { hospitalId, department, doctorCode } = req.body;
    const today = new Date().toISOString().split('T')[0];

    // 1. Verify the doctor exists and is assigned to this department and hospital
    const doctor = await UserV2.findOne({ doctorCode, hospitalId, role: 'DOCTOR' });
    if (!doctor) {
      return res.status(404).json({ success: false, message: 'Doctor not found in this hospital.' });
    }

    if (doctor.department !== department) {
      return res.status(400).json({ 
        success: false, 
        message: `Doctor belongs to the ${doctor.department} department, not ${department}.` 
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

    // 3. Create the queue
    const newQueue = await QueueV2.create({
      hospitalId,
      department,
      doctorCode,
      date: today,
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
module.exports={
    createDepartmentQueue,
}
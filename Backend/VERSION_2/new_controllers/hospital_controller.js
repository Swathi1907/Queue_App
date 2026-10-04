const HospitalV2 = require('../new_models/new_hosp_model');
const UserV2= require('../new_models/peron_model')
const crypto = require('crypto');
const jwt = require('jsonwebtoken');
const QueueV2 = require('../new_models/new_queuev2'); // Your new_queueV2 model
// Inside your verifyDoctorCode controller:
const mongoose = require('mongoose')
const generateHospitalCode = (name) => {
  const prefix = name
    .split(' ')
    .map((word) => word[0])
    .join('')
    .toUpperCase()
    .replace(/[^A-Z]/g, '')
    .slice(0, 4);

  const randomDigits = crypto.randomInt(1000, 9999);
  return `${prefix}-${randomDigits}`;
};


// @desc    Get single hospital details by ID
// @route   GET /api/v2/hospital/:id
// @access  Public
const getHospitalById = async (req, res) => {
    try {
      console.log("called hosp");
        console.log("admin hospital details hit");
        let hospitalId = req.params.hospitalId || req.user?.hospitalId;
        
        if (typeof hospitalId === 'string') {
            hospitalId = hospitalId.trim();
        }

        if (!hospitalId) {
            return res.status(400).json({
                success: false,
                message: 'Hospital ID is required.'
            });
        }

        // Query by your fixed code/identifier field used in settings
        const hospital = await HospitalV2.findOne({ code: hospitalId });

        if (!hospital) {
            return res.status(404).json({
                success: false,
                message: 'Hospital profile not found.'
            });
        }
console.log("success hit")
console.log(hospital);
        return res.status(200).json({
            success: true,
            data: hospital
        });

    } catch (error) {
        console.error("Error fetching hospital details for admin:", error.message);
        return res.status(500).json({
            success: false,
            message: 'Server Error',
            error: error.message
        });
    }
};
const createHospital = async (req, res) => {
  try {
    const { name, contactNumber, email, address, description, departments, latitude, longitude } = req.body;

    if (!name || !contactNumber) {
      return res.status(400).json({
        success: false,
        message: 'Hospital name and contact number are required.',
      });
    }

    // Validate that coordinates are provided for the map and geospatial queries
    if (latitude === undefined || longitude === undefined) {
      return res.status(400).json({
        success: false,
        message: 'Latitude and longitude are required for hospital location.',
      });
    }

    let code = generateHospitalCode(name);
    let isCodeUnique = false;

    while (!isCodeUnique) {
      const existingHospital = await HospitalV2.findOne({ code });
      if (!existingHospital) {
        isCodeUnique = true;
      } else {
        code = generateHospitalCode(name);
      }
    }

    const hospital = await HospitalV2.create({
      name,
      code,
      contactNumber,
      email,
      address,
      description,
      departments,
      // GeoJSON requires longitude first, then latitude: [lng, lat]
      location: {
        type: 'Point',
        coordinates: [parseFloat(longitude), parseFloat(latitude)],
      },
    });

    res.status(201).json({
      success: true,
      message: 'Hospital created successfully',
      data: hospital,
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: error.message || 'Server Error while creating hospital',
    });
  }
};
const getHospitalDepartments = async (req, res) => {
  try {
    const { hospitalId } = req.params;

    const query = mongoose.Types.ObjectId.isValid(hospitalId)
      ? { _id: hospitalId }
      : { code: hospitalId };

    const hospital = await HospitalV2.findOne(query);
    if (!hospital) {
      return res.status(404).json({ success: false, message: 'Hospital not found with that code.' });
    }

    // 1. Doctors of this hospital (same filter as the doctors endpoint)
    const doctors = await UserV2.find({
      $or: [
        { hospitalId: hospital._id.toString() },
        { hospitalId: hospital.code }
      ],
      role: 'DOCTOR'
    }).lean();

    // doctorCode (or _id fallback) -> lowercase department
    const doctorDept = new Map(
      doctors.map(d => [
        String(d.doctorCode || d._id),
        String(d.department || '').trim().toLowerCase()
      ])
    );

    // 2. Queues for those doctors, newest first
    const queues = await QueueV2.find({
      doctorCode: { $in: [...doctorDept.keys()] }
    }).sort({ createdAt: -1 }).lean();

    // 3. Count active queues per department (one newest queue per doctor)
    const seen = new Set();
    const countMap = new Map();

    for (const q of queues) {
      const code = String(q.doctorCode);
      if (seen.has(code)) continue;
      seen.add(code);

      const isActive =
        q.queueStatus !== 'CLOSED' && q.queueStatus !== 'PAUSED' && q.isActive !== false;
      if (!isActive) continue;

      const dept = doctorDept.get(code);
      if (!dept) continue;
      countMap.set(dept, (countMap.get(dept) || 0) + 1);
    }

    // 4. ALL departments, each with its queue count (0 if none)
    const departments = (hospital.departments || []).map(name => ({
      name,
      waitingCount: countMap.get(String(name).trim().toLowerCase()) || 0 // waiting count-> it is queueCount to match frontend
    }));
for(let i=0;i<departments.length;i++){
  console.log(departments[i].name,departments[i].waitingCount);
}
    return res.status(200).json({
      success: true,
      data: {
        hospitalCode: hospital.code,
        hospitalName: hospital.name,
        departments
      }
    });
  } catch (error) {
    console.error(error.message);
    return res.status(500).json({ success: false, error: error.message });
  }
};
const addDepartmentsToHospital = async (req, res) => {
  try {
    const { hospitalId } = req.params;
    const { departments } = req.body; // Expects an array of strings, e.g., ["Cardiology", "Neurology"]

    if (!Array.isArray(departments) || departments.length === 0) {
      return res.status(400).json({ success: false, message: 'Please provide an array of departments.' });
    }

    // Find hospital and add unique departments using $addToSet
    const hospital = await HospitalV2.findOneAndUpdate(
      { code: hospitalId },
      { $addToSet: { departments: { $each: departments } } },
      { new: true, runValidators: true }
    );

    if (!hospital) {
      return res.status(404).json({ success: false, message: 'Hospital not found with that code.' });
    }

    return res.status(200).json({
      success: true,
      message: 'Departments added successfully.',
      data: {
        hospitalCode: hospital.code,
        hospitalName: hospital.name,
        departments: hospital.departments
      }
    });

  } catch (error) {
    return res.status(500).json({ success: false, error: error.message });
  }
};
// Step 1: Verify Hospital & Authenticate User Credentials
const verifyHospitalId = async (req, res) => {
  try {
    console.log("hitting");
    const { email, password } = req.body;
    const hospitalId = req.body.hospitalId;

    if (!email || !password || !hospitalId) {
      return res.status(400).json({
        success: false,
        message: 'Email, password, and Hospital ID are required for verification.',
      });
    }

    const user = await UserV2.findOne({ email }).select('+password');
    if (!user) {
      return res.status(404).json({
        success: false,
        message: 'User not found with this email.',
      });
    }

    const isMatch = await user.comparePassword(password);
    if (!isMatch) {
      return res.status(401).json({
        success: false,
        message: 'Invalid email or password.',
      });
    }

    const hospital = await HospitalV2.findOne({ code: hospitalId });
    if (!hospital) {
      return res.status(404).json({
        success: false,
        message: `Hospital not found with code: ${hospitalId}`,
      });
    }

    if (user.hospitalId && user.hospitalId !== hospital.code && user.role !== 'ADMIN') {
      return res.status(403).json({
        success: false,
        message: 'User does not belong to this hospital.',
      });
    }

    console.log("User role:", user.role);

    // If user is a DOCTOR, inform Android that Step 1 is verified and Step 2 is required
    if (user.role === 'DOCTOR') {
      return res.status(200).json({
        success: true,
        requiresDoctorCode: true,
        message: 'Hospital and credentials verified. Please enter Doctor Code.',
        data: {
          role: user.role,
          hospitalId: hospital.code,
          user: {
            _id: user._id,
            name: user.name,
            email: user.email
          }
        }
      });
    }

    // For non-doctors, generate the final token immediately
    const jwt_token = jwt.sign(
      { 
        id: user._id, 
        email: user.email, 
        role: user.role, 
        hospitalId: hospital.code 
      },
      process.env.JWT_SECRET,
      { expiresIn: '7d' }
    );

    return res.status(200).json({
      success: true,
      requiresDoctorCode: false,
      message: 'Hospital and credentials verified successfully!',
      data: {
        jwt_token,
        role: user.role,
        hospitalId: hospital.code,
        user: {
          _id: user._id,
          name: user.name,
          email: user.email
        }
      }
    });

  } catch (error) {
    if (res && !res.headersSent) {
      return res.status(500).json({ success: false, error: error.message });
    }
    throw error;
  }
};


const getDoctorsByDepartment = async (req, res) => {
  try {
    const { hospitalId, departmentName } = req.params;

    if (!hospitalId || !departmentName) {
      return res.status(400).json({
        success: false,
        message: 'Hospital ID and Department name are required.',
      });
    }

    const decodedDepartment = decodeURIComponent(departmentName);

    // Using an explicit inclusion/exclusion object to prevent projection collisions
    const doctors = await UserV2.find({
      role: 'DOCTOR',
      hospitalId: hospitalId,
      department: decodedDepartment,
      isActive: true,
    }).select({ password: 0 }); // Exclude only the password, leaving all other fields (including doctorCode) included by default

    return res.status(200).json({
      success: true,
      count: doctors.length,
      data: doctors,
    });
  } catch (error) {
    console.error('Error fetching doctors by department name:', error);
    return res.status(500).json({
      success: false,
      message: 'Internal server error while fetching doctors.',
    });
  }
}; 


// Ensure your Doctor model path is correct
// Adjust path to your queue model
const getUserSideDoctorsByDepartment = async (req, res) => {
    try {
        const { hospitalId, departmentName } = req.params;
        const currentUserId = req.user?.id || req.query.userId; 
        console.log("Current User ID:", currentUserId);
        console.log(`Fetching doctors with queues for hospital: ${hospitalId}, department: ${departmentName}`);

        let hospitalQuery;
        if (mongoose.Types.ObjectId.isValid(hospitalId)) {
            hospitalQuery = { _id: hospitalId };
        } else {
            hospitalQuery = { code: hospitalId };
        }

        const hospital = await HospitalV2.findOne(hospitalQuery);
        if (!hospital) {
            return res.status(404).json({
                success: false,
                message: 'Hospital not found'
            });
        }

        const doctors = await UserV2.find({
            $or: [
                { hospitalId: hospital._id.toString() },
                { hospitalId: hospital.code }
            ],
            role: 'DOCTOR',
            department: { $regex: new RegExp(`^${departmentName}$`, 'i') }
        });

       const formattedDoctors = (await Promise.all(doctors.map(async (doc) => {
    const doctorCodeVal = doc.doctorCode || doc._id.toString();

    // Prefer the ACTIVE queue; only fall back to latest if none is active
    let queue = await QueueV2.findOne({
        doctorCode: doctorCodeVal,
        queueStatus: "ACTIVE"
    }).sort({ createdAt: -1 });

    if (!queue) {
        queue = await QueueV2.findOne({ doctorCode: doctorCodeVal })
            .sort({ createdAt: -1 });
    }
    if (!queue) return null;

    const isActive = queue.queueStatus === "ACTIVE";
    const uid = currentUserId ? currentUserId.toString() : null;
    const tokens = Array.isArray(queue.tokens) ? queue.tokens : [];

    const isMine = (t) =>
        uid && (t.userId?.toString() === uid || t.patientId?.toString() === uid);

    // Only count waiting tokens in an active queue, excluding the user's own
    const peopleAheadCount = isActive
        ? tokens.filter(t => t.status === 'WAITING' && !isMine(t)).length
        : 0;

    const userHasJoined = tokens.some(t =>
        isMine(t) && t.status !== 'CANCELLED' && t.status !== 'COMPLETED'
    );

    const avgServiceTimeMinutes = queue.avgServiceTime || 5;
    const calculatedWaitMinutes = peopleAheadCount * avgServiceTimeMinutes;
    const waitTimeText = peopleAheadCount === 0 ? "No wait" : `~${calculatedWaitMinutes} mins`;

    return {
        _id: doc._id,
        doctorCode: doctorCodeVal,
        name: doc.name,
        specialty: doc.qualification || departmentName,
        imageUrl: doc.imageUrl || "",
        consultationFee: doc.consultationFee || 100,
        peopleAhead: peopleAheadCount,
        avgServiceTime: `${avgServiceTimeMinutes} mins/patient`,
        estimatedWaitTime: waitTimeText,
        isQueuePaused: !isActive,
        isJoined: userHasJoined
    };
}))).filter(Boolean);

        return res.status(200).json({
            success: true,
            count: formattedDoctors.length,
            data: formattedDoctors
        });

    } catch (error) {
        console.error("Error fetching doctors by department:", error);
        return res.status(500).json({
            success: false,
            message: 'Server Error',
            error: error.message
        });
    }
};

const updateHospitalDetails = async (req, res) => {
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

    const { hospitalName, description, phone, address, bannerImageUrl } = req.body;

    const updateFields = {};
    if (hospitalName) updateFields.name = hospitalName; // Updated to match schema 'name'
    if (description) updateFields.description = description;
    if (phone) updateFields.contactNumber = phone; // Updated to match schema 'contactNumber'
    if (address) updateFields.address = address;
    if (bannerImageUrl) updateFields.imageUrl = bannerImageUrl; // Updated to match schema 'imageUrl'

    // Fixed query from hospitalCode to code
    const updatedHospital = await HospitalV2.findOneAndUpdate(
      { code: hospitalId },
      { $set: updateFields },
      { new: true, runValidators: true }
    );

    if (!updatedHospital) {
      return res.status(404).json({
        success: false,
        message: 'Hospital profile not found.'
      });
    }

    return res.status(200).json({
      success: true,
      message: 'Hospital details updated successfully',
      data: updatedHospital
    });

  } catch (error) {
    console.log("Error updating hospital details:", error.message);
    return res.status(500).json({ success: false, error: error.message });
  }
};

// Get all active hospitals// Get all active hospitals
const getAllHospitals = async (req, res) => {
    try {
        const hospitals = await HospitalV2.find({ isActive: true });
        
        // Map fields to match your Kotlin Hospital data model structure
        const formattedHospitals = hospitals.map(hospital => ({
            _id: hospital._id,
            name: hospital.name,
            code: hospital.code,
            contactNumber: hospital.contactNumber,
            email: hospital.email,
            address: hospital.address, 
            location: hospital.location, // <-- Add this line to include GeoJSON location [longitude, latitude]
            departments: hospital.departments,
            isActive: hospital.isActive,
            createdAt: hospital.createdAt,
            updatedAt: hospital.updatedAt
        }));

        res.status(200).json({
            success: true,
            count: formattedHospitals.length,
            data: formattedHospitals
        });
    } catch (error) {
        res.status(500).json({
            success: false,
            message: 'Server Error',
            error: error.message
        });
    }
};
const verifyDoctorCode = async (req, res) => {
  try {
    console.log("verify doctor hit");
    const { doctorCode, hospitalId } = req.body;
    
    // Fixed query from hospitalCode to code
    const hospital = await HospitalV2.findOne({ code: hospitalId });

    if (!doctorCode) {
      return res.status(400).json({
        success: false,
        message: 'Doctor code is required for verification.',
      });
    }

    const doctor = await UserV2.findOne({ 
      role: 'DOCTOR', 
      hospitalId: hospitalId, 
      doctorCode: doctorCode.toUpperCase() 
    }).select('-password');

    if (!doctor) {
      return res.status(404).json({
        success: false,
        message: `Doctor with code '${doctorCode}' not found in hospital '${hospital?.name || hospitalId}'.`,
      });
    }

    const jwt_token = jwt.sign(
      { 
        id: doctor._id, 
        email: doctor.email, 
        role: doctor.role, 
        hospitalId: hospitalId
      },
      process.env.JWT_SECRET,
      { expiresIn: '7d' }
    );

    return res.status(200).json({
      success: true,
      message: 'Doctor code verified successfully!',
      data: {
        jwt_token,
        role: doctor.role,
        hospitalId: hospitalId,
        doctorCode: doctor.doctorCode,
        doctor: {
          _id: doctor._id,
          name: doctor.name,
          department: doctor.department,
          qualification: doctor.qualification,
          isAvailable: doctor.isAvailable
        }
      },
    });
  } catch (error) {
    console.log(error.message);
    return res.status(500).json({ success: false, error: error.message });
  }
};

module.exports = {
 createHospital,
 getHospitalDepartments,
 addDepartmentsToHospital,
getDoctorsByDepartment,
verifyHospitalId,
verifyDoctorCode,
getAllHospitals,
getHospitalById,
getUserSideDoctorsByDepartment,
updateHospitalDetails
};


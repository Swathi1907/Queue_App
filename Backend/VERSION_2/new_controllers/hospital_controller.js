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
      console.log("by id hit")
      console.log(req.params.hospitalId)
        const hospital = await HospitalV2.findById(req.params.hospitalId);

        if (!hospital) {
         console.log("not found")
            return res.status(404).json({
                success: false,
                message: 'Hospital not found or inactive'
            });
        }

        // Format the response to match your Android app's HospitalDetailItem model
        const formattedHospital = {
            _id: hospital._id,
            name: hospital.name,
            code: hospital.code,
            address: `${hospital.address.street || ''}, ${hospital.address.city}, ${hospital.address.state}`.trim(),
            distance: "1.2 km away", // Can be calculated dynamically or mocked
            rating: 4.8,
            reviewsCount: 124,
            waitTime: "Short wait time",
            imageUrl: hospital.imageUrl || ""
        };

        res.status(200).json({
            success: true,
            data: formattedHospital
        });
    } catch (error) {
      
        res.status(500).json({
            success: false,
            message: 'Server Error',
            error: error.message
        });
    }
};


const createHospital = async (req, res) => {
  try {
    const { name, contactNumber, email, address } = req.body;

    if (!name || !contactNumber) {
      return res.status(400).json({
        success: false,
        message: 'Hospital name and contact number are required.',
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
    console.log("get hit");
    const { hospitalId } = req.params;
let query;
    if (mongoose.Types.ObjectId.isValid(hospitalId)) {
      query = { _id: hospitalId };
    } else {
      query = { code: hospitalId };
    }
    // 1. Find the hospital by code
    const hospital = await HospitalV2.findOne(query);
    if (!hospital) {
      
      return res.status(404).json({ success: false, message: 'Hospital not found with that code.' });
    }

    const departmentsList = hospital.departments || [];

    // 2. For each department, aggregate the waiting tokens across all doctor queues for this hospital
    const departmentsWithCounts = await Promise.all(
      departmentsList.map(async (deptName) => {
        const result = await QueueV2.aggregate([
          {
            $match: {
              hospitalId: hospitalId,
              department: deptName,
              isActive: true
            }
          },
          { $unwind: "$tokens" },
          {
            $match: {
              "tokens.status": "WAITING"
            }
          },
          {
            $count: "waitingCount"
          }
        ]);

        // If no matching documents/tokens are found, count is 0
        const waitingCount = result.length > 0 ? result[0].waitingCount : 0;

        return {
          name: deptName,
          waitingCount: waitingCount
        };
      })
    );

    // 3. Return the formatted data matching your Android expectations
    return res.status(200).json({
      success: true,
      data: {
        hospitalCode: hospital.code,
        hospitalName: hospital.name,
        departments: departmentsWithCounts
      }
    });

  } catch (error) {
    console.log(error.message);
    return res.status(500).json({ success: false, error: error.message });
  }
};


/*

const getHospitalDepartments = async (req, res) => {
  try {
    console.log("get hit")
    const { hospitalId } = req.params; // Or req.query, depending on your route design

    const hospital = await HospitalV2.findOne({ code: hospitalId });
    if (!hospital) {
      return res.status(404).json({ success: false, message: 'Hospital not found with that code.' });
    }
console.log(hospital.departments)
    return res.status(200).json({
      success: true,
      data: {
        hospitalCode: hospital.code,
        hospitalName: hospital.name,
        departments: hospital.departments || []
      }
    });

  } catch (error) {
    console.log(error.message)
    return res.status(500).json({ success: false, error: error.message });
  }
}; */
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
        console.log(`Fetching doctors for hospital: ${hospitalId}, department: ${departmentName}`);

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

        const todayDate = new Date().toISOString().split('T')[0];

        // Format doctors and fetch real-time queue metrics for each
        const formattedDoctors = await Promise.all(doctors.map(async (doc) => {
            const activeQueue = await QueueV2.findOne({ 
                doctorCode: doc.doctorCode || doc._id.toString(), 
                date: todayDate,
                isActive: true 
            });

            let peopleAheadCount = 0;
            if (activeQueue && activeQueue.tokens) {
                peopleAheadCount = activeQueue.tokens.filter(t => t.status === 'WAITING').length;
            }

            const calculatedWaitMinutes = peopleAheadCount * 15;
            const waitTimeText = peopleAheadCount === 0 ? "No wait" : `~${calculatedWaitMinutes} mins`;

            return {
                _id: doc._id,
                doctorCode: doc.doctorCode || doc._id.toString(),
                name: doc.name,
                specialty: doc.qualification || departmentName,
                imageUrl: doc.imageUrl || "",
                consultationFee: doc.consultationFee || 100, // Included consultation fee (stored in INR)
                peopleAhead: peopleAheadCount,
                estimatedWaitTime: waitTimeText
            };
        }));

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
/*
const getUserSideDoctorsByDepartment = async (req, res) => {
    try {
        const { hospitalId, departmentName } = req.params;
        console.log(`Fetching doctors for hospital: ${hospitalId}, department: ${departmentName}`);

        // Resolve hospital query (supporting both ObjectId and custom string code)
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
console.log("hosp found")
console.log(hospital._id)
        // Query UserV2 collection for users with role 'DOCTOR' belonging to this hospital 
        // and whose department array contains the matching department name (case-insensitive)
     console.log("Querying with:", {
    hospitalId: hospital._id.toString(),
    role: 'DOCTOR',
    department: departmentName
});

const doctors = await UserV2.find({
            $or: [
                { hospitalId: hospital._id.toString() },
                { hospitalId: hospital.code }
            ],
            role: 'DOCTOR',
            department: { $regex: new RegExp(`^${departmentName}$`, 'i') }
        });
console.log("Raw doctors found in DB:", doctors);
        console.log("docotors found")
        // Format the response data to match your Android app's UserDoctorItem expectations
        const formattedDoctors = doctors.map(doc => ({
            _id: doc._id,
            name: doc.name,
            specialty: doc.qualification || departmentName, // Falls back to qualification or department name
            imageUrl: doc.imageUrl || "",
            peopleAhead: doc.peopleAhead || 0,
            estimatedWaitTime: doc.estimatedWaitTime || "15 mins"
        }));
console.log(formattedDoctors)
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

*/

// Get all active hospitals
const getAllHospitals = async (req, res) => {
    try {
        const hospitals = await HospitalV2.find({ isActive: true });
        
        // Optional: Format the response to fit your UI display requirements
        const formattedHospitals = hospitals.map(hospital => ({
            _id: hospital._id,
            name: hospital.name,
            code: hospital.code,
            address: `${hospital.address.street || ''}, ${hospital.address.city}, ${hospital.address.state}`.trim(),
            distance: "1.2 km away", // You can calculate dynamically or mock for now
            rating: 4.8,             // Add fields if stored or map default UI placeholders
            reviewsCount: 124,
            waitTime: "Short wait time",
            imageUrl: ""             // Add image URL field to schema if needed
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
/*const getDoctorsByDepartment = async (req, res) => {
  try {
    const { hospitalId, departmentName } = req.params;

    if (!hospitalId || !departmentName) {
      return res.status(400).json({
        success: false,
        message: 'Hospital ID and Department name are required.',
      });
    }

    // Decode URI component in case the department name has spaces (e.g., "General Medicine")
    const decodedDepartment = decodeURIComponent(departmentName);

   const doctors = await UserV2.find({
      role: 'DOCTOR',
      hospitalId: hospitalId,
      department: decodedDepartment,
      isActive: true,
    }).select({ password: 0, doctorCode: 1 });

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
*/

// Step 2: Verify Doctor Code within validated hospital context
const verifyDoctorCode = async (req, res) => {
  try {
    console.log("verify docotr hit")
    const { email, password, doctorCode,hospitalId } = req.body;
    const hospital = await HospitalV2.findOne({hospitalCode:hospitalId});

console.log(req.body);
    if (!doctorCode) {
      return res.status(400).json({
        success: false,
        message: 'Doctor code is required for verification.',
      });
    }

    // Find doctor linked to this hospital code and matching the doctor code
    const doctor = await UserV2.findOne({ 
      role: 'DOCTOR', 
      hospitalId: hospitalId, 
      doctorCode: doctorCode.toUpperCase() 
    }).select('-password');
console.log(doctor)
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
    // Generate JWT or return final authorization payload here if needed
    // const jwt_token = generateAuthToken(doctor);
console.log("success")
    return res.status(200).json({
      success: true,
      message: 'Doctor code verified successfully!',
      data: {
        jwt_token, // Include your token generation logic
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
    console.log(error.message)
    return res.status(500).json({ success: false, error: error.message });
  }
};
// Middleware or helper to verify hospital existence by code
/*
const verifyHospitalId = async (req, res, next) => {
  try {
    console.log("verify")
    // Check multiple locations and alias keys (like your Postman payload)
    const hospitalId = 
      req.body.hospitalId || 
      req.body["Hospital ID(code)"] || 
      req.params.hospitalId || 
      req.query.hospitalId;

    if (!hospitalId) {
      return res.status(400).json({
        success: false,
        message: 'Hospital ID (code) is required for verification.',
      });
    }

    const hospital = await HospitalV2.findOne({ code: hospitalId });
    
    if (!hospital) {
      return res.status(404).json({
        success: false,
        message: `Hospital not found with code: ${hospitalId}`,
      });
    }

    // Attach hospital to request object so downstream controllers can use it
    req.hospital = hospital;
    
    // CRITICAL: Call next() so Express moves on to the actual controller function
    return next();
  } catch (error) {
    if (res && !res.headersSent) {
      return res.status(500).json({ success: false, error: error.message });
    }
    throw error;
  }
};

// Verify doctor code within a validated hospital context
const verifyDoctorCode = async (req, res) => {
  try {
    const { doctorCode } = req.body;
    
    // req.hospital is already validated and attached by verifyHospitalId middleware
    const hospital = req.hospital;

    if (!doctorCode) {
      return res.status(400).json({
        success: false,
        message: 'Doctor code is required for verification.',
      });
    }

    // Find doctor linked to this hospital code
    const doctor = await UserV2.findOne({ 
      role: 'DOCTOR', 
      hospitalId: hospital.code, 
      doctorCode: doctorCode.toUpperCase() 
    }).select('-password');

    if (!doctor) {
      return res.status(404).json({
        success: false,
        message: `Doctor with code '${doctorCode}' not found in hospital '${hospital.name}'.`,
      });
    }

    return res.status(200).json({
      success: true,
      message: 'Doctor code verified successfully!',
      data: {
        hospitalCode: hospital.code,
        hospitalName: hospital.name,
        doctor: {
          _id: doctor._id,
          name: doctor.name,
          doctorCode: doctor.doctorCode,
          department: doctor.department,
          qualification: doctor.qualification,
          isAvailable: doctor.isAvailable
        }
      },
    });
  } catch (error) {
    return res.status(500).json({ success: false, error: error.message });
  }
};
*/

module.exports = {
 createHospital,
 getHospitalDepartments,
 addDepartmentsToHospital,
getDoctorsByDepartment,
verifyHospitalId,
verifyDoctorCode,
getAllHospitals,
getHospitalById,
getUserSideDoctorsByDepartment
};


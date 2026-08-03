const User = require('../new_models/peron_model');
const jwt = require('jsonwebtoken');
const bcrypt = require('bcryptjs');
const HospitalV2 = require('../new_models/new_hosp_model');

const generateToken = (id, role) => {
  return jwt.sign({ id, role }, process.env.JWT_SECRET, { expiresIn: '30d' });
};

// 1. Register Patient

const registerUser = async (req, res, next) => {
  try {
    console.log("Register called");
    const { name, email, phoneNumber, password } = req.body;

    const queryConditions = [{ phoneNumber }];
    if (email) queryConditions.push({ email });

    const existingUser = await User.findOne({ $or: queryConditions });
    if (existingUser) {
      return res.status(400).json({
        success: false,
        message: 'User with this phone number or email already exists',
      });
    }

    // Hash password manually since schema has no pre-save hook
    const salt = await bcrypt.genSalt(10);
    const hashedPassword = await bcrypt.hash(password, salt);

    const user = await User.create({
      name,
      email: email || null,
      phoneNumber,
      password: hashedPassword,
      role: 'PATIENT',
      hospitalId: null,
      department: null,
    });

    const jwt_token = generateToken(user._id, user.role);
console.log(jwt_token)
    return res.status(201).json({
      success: true,
      message: 'Patient account registered successfully',
      data: {
        _id: user._id,
        name: user.name,
        email: user.email,
        phoneNumber: user.phoneNumber,
        role: user.role,
        hospitalId: user.hospitalId,
        department: user.department,
        doctorCode: user.doctorCode,
        qualification: user.qualification,
        rating: user.rating,
        isAvailable: user.isAvailable,
        isActive: user.isActive,
        jwt_token,
      },
    });
  } catch (error) {
    console.log(err.message);
    return res.status(500).json({ success: false, error: error.message });
  }
};

// 2. Create Staff User
const createStaffUser = async (req, res, next) => {
  try {
    const { name, email, phoneNumber, password, role, hospitalId, department, qualification, doctorCode } = req.body;

    if (!role || role === 'PATIENT') {
      return res.status(400).json({
        success: false,
        message: 'Use public registration endpoint for patients.',
      });
    }

    const queryConditions = [{ phoneNumber }];
    if (email) queryConditions.push({ email });

    const existingUser = await User.findOne({ $or: queryConditions });
    if (existingUser) {
      return res.status(400).json({
        success: false,
        message: 'User with this phone number or email already exists',
      });
    }

    // Hash password manually
    const salt = await bcrypt.genSalt(10);
    const hashedPassword = await bcrypt.hash(password, salt);

    const staffUser = await User.create({
      name,
      email: email || null,
      phoneNumber,
      password: hashedPassword,
      role,
      hospitalId: hospitalId || req.user?.hospitalId || null,
      department: department || null,
      qualification: qualification || null,
      doctorCode: doctorCode || null,
    });

    return res.status(201).json({
      success: true,
      message: `${role} account created successfully`,
      data: {
        _id: staffUser._id,
        name: staffUser.name,
        phoneNumber: staffUser.phoneNumber,
        role: staffUser.role,
        hospitalId: staffUser.hospitalId,
        department: staffUser.department,
      },
    });
  } catch (error) {
    return res.status(500).json({ success: false, error: error.message });
  }
};

// 3. Login User
const loginUser = async (req, res) => {
  try {
    console.log("login")
    const { email, password } = req.body;

    if (!email || !password) {
      return res.status(400).json({
        success: false,
        message: 'Please provide email and password',
      });
    }

    const cleanEmail = email.trim().toLowerCase();
    const user = await User.findOne({ email: cleanEmail }).select('+password');
    
    if (!user) {
      console.log('Login failed: User not found for email:', email);
      return res.status(401).json({ success: false, message: 'Invalid credentials' });
    }

    // Uses the schema's comparePassword method or direct bcrypt check
    const isMatch = await user.comparePassword(password);
    if (!isMatch) {
      console.log('Login failed: Password mismatch for email:', email);
      return res.status(401).json({ success: false, message: 'Invalid credentials' });
    }

    const jwt_token = generateToken(user._id, user.role);
console.log(jwt_token)
   return res.status(200).json({
      success: true,
      message: 'Login successful',
      data: {
        _id: user._id,
        name: user.name,
        email: user.email,
        phoneNumber: user.phoneNumber,
        role: user.role,
        hospitalId: user.hospitalId,
        department: user.department,
        doctorCode: user.doctorCode,
        qualification: user.qualification,
        rating: user.rating,
        isAvailable: user.isAvailable,
        isActive: user.isActive,
        jwt_token,
      },
    });
  } catch (error) {
    return res.status(500).json({ success: false, error: error.message });
  }
};

// 4. Register Doctor (Triggered by Superadmin)
/*const registerDoctor = async (req, res) => {
  try {
    const { name, email, phoneNumber, password, hospitalId, department, qualification, rating } = req.body;

    const hospital = await HospitalV2.findOne({ code: hospitalId });
    if (!hospital) {
      return res.status(404).json({ success: false, message: 'Hospital not found with that code.' });
    }

    if (!hospital.departments || !hospital.departments.includes(department)) {
      return res.status(400).json({ 
        success: false, 
        message: `Department '${department}' does not exist in this hospital.` 
      });
    }

    const doctorCount = await User.countDocuments({ role: 'DOCTOR', hospitalId });
    const doctorCode = `DOC-${hospitalId}-${String(doctorCount + 1).padStart(3, '0')}`;

    // Hash password manually
    const salt = await bcrypt.genSalt(10);
    const hashedPassword = await bcrypt.hash(password, salt);

    const newDoctor = await User.create({
      name,
      email: email || null,
      phoneNumber,
      password: hashedPassword,
      role: 'DOCTOR',
      hospitalId,
      department,
      doctorCode,
      qualification,
      rating: rating || 5.0
    });

    const doctorResponse = newDoctor.toObject();
    delete doctorResponse.password;

    return res.status(201).json({
      success: true,
      message: 'Doctor account created successfully with auto-generated code.',
      data: doctorResponse
    });

  } catch (error) {
    if (error.code === 11000) {
      return res.status(400).json({ 
        success: false, 
        message: 'Phone number or doctor code already exists.' 
      });
    }
    return res.status(500).json({ success: false, error: error.message });
  }
};
*/
const registerDoctor = async (req, res) => {
  try {
    // Note: Expecting 'departments' as an array now (e.g., ["Cardiology", "General Medicine"])
    const { name, email, phoneNumber, password, hospitalId, departments, qualification, rating } = req.body;

    const hospital = await HospitalV2.findOne({ code: hospitalId });
    if (!hospital) {
      return res.status(404).json({ success: false, message: 'Hospital not found with that code.' });
    }

    // Validate that 'departments' is provided and is a non-empty array
    if (!Array.isArray(departments) || departments.length === 0) {
      return res.status(400).json({ 
        success: false, 
        message: 'At least one department must be provided for a doctor.' 
      });
    }

    // Verify that every requested department exists in the hospital's catalog
    if (!hospital.departments) {
      return res.status(400).json({ 
        success: false, 
        message: 'This hospital has no departments registered.' 
      });
    }

    const invalidDepartments = departments.filter(dept => !hospital.departments.includes(dept));
    if (invalidDepartments.length > 0) {
      return res.status(400).json({ 
        success: false, 
        message: `The following department(s) do not exist in this hospital: ${invalidDepartments.join(', ')}` 
      });
    }

    const doctorCount = await User.countDocuments({ role: 'DOCTOR', hospitalId });
    const doctorCode = `DOC-${hospitalId}-${String(doctorCount + 1).padStart(3, '0')}`;

    // Hash password manually
    const salt = await bcrypt.genSalt(10);
    const hashedPassword = await bcrypt.hash(password, salt);

    const newDoctor = await User.create({
      name,
      email: email || null,
      phoneNumber,
      password: hashedPassword,
      role: 'DOCTOR',
      hospitalId,
      department: departments, // Assign the array of departments matching your updated schema
      doctorCode,
      qualification,
      rating: rating || 5.0
    });

    const doctorResponse = newDoctor.toObject();
    delete doctorResponse.password;

    return res.status(201).json({
      success: true,
      message: 'Doctor account created successfully with multiple departments.',
      data: doctorResponse
    });

  } catch (error) {
    if (error.code === 11000) {
      return res.status(400).json({ 
        success: false, 
        message: 'Phone number or doctor code already exists.' 
      });
    }
    return res.status(500).json({ success: false, error: error.message });
  }
};
module.exports = {
  registerDoctor,
  registerUser,
  createStaffUser,
  loginUser,
};
const express = require('express');
const router = express.Router();
const { 
  createHospital, 
  addDepartmentsToHospital, 
  verifyHospitalId, 
  getHospitalDepartments,
  getDoctorsByDepartment,
  verifyDoctorCode,
  getAllHospitals,
  getHospitalById,
  getUserSideDoctorsByDepartment
} = require('../new_controllers/hospital_controller');

const { authmiddleware, authorize } = require('../new_middleware/authmiddleware');

// -------------------------------------------------------------
// 1. SPECIFIC / STATIC ROUTES (Must come BEFORE dynamic params)
// -------------------------------------------------------------

// Protect hospital creation (SUPER_ADMIN only)
router.post('/Create', authmiddleware, authorize('SUPER_ADMIN'), createHospital);

// Verify hospital existence by ID/Code
router.post('/verifyHospitalId', authmiddleware, verifyHospitalId, (req, res) => {
  return res.status(200).json({
    success: true,
    message: 'Hospital verified successfully!',
    data: req.hospital,
  });
});

// Standalone Doctor Code verification route
router.post('/verifyDoctorCode', authmiddleware, verifyDoctorCode, (req, res) => {
  return res.status(200).json({
    success: true,
    message: 'Doctor verified successfully!',
    data: req.authData,
  });
});

// Static GET routes (Must stay above dynamic :hospitalId routes)
router.get('/getAllHospitals', getAllHospitals);

// -------------------------------------------------------------
// 2. DYNAMIC / PARAMETERIZED ROUTES (Place at the bottom)
// -------------------------------------------------------------

// Get department list for a hospital
router.get('/:hospitalId/getDepartments', getHospitalDepartments);

// Add departments to a hospital
router.put('/:hospitalId/addDepartments', addDepartmentsToHospital);

// Get specific hospital details by ID or code
router.get('/:hospitalId/getHospital', getHospitalById);

// Get doctors by department (Admin/General view)
router.get('/:hospitalId/departments/:departmentName/doctors', getDoctorsByDepartment);

// Get user-side doctors by department
router.get('/:hospitalId/departments/:departmentName/Usersidedoctors', getUserSideDoctorsByDepartment);

module.exports = router;
const express = require('express');
const router = express.Router();
const { 
  createHospital, 
  addDepartmentsToHospital, 
  verifyHospitalId, 
  getHospitalDepartments ,
  getDoctorsByDepartment,
  verifyDoctorCode
} = require('../new_controllers/hospital_controller');

const { authmiddleware, authorize } = require('../new_middleware/authmiddleware');

// -------------------------------------------------------------
// 1. SPECIFIC / STATIC ROUTES (Must come BEFORE dynamic params)
// -------------------------------------------------------------

// Protect hospital creation (SUPER_ADMIN only)
router.post('/Create', authmiddleware, authorize('SUPER_ADMIN'), createHospital);

// Verify hospital existence by ID/Code
// 1. Standalone Hospital Verification route (used by Compounders / Staff step 1)
router.post('/verifyHospitalId',authmiddleware, verifyHospitalId, (req, res) => {
  return res.status(200).json({
    success: true,
    message: 'Hospital verified successfully!',
    data: req.hospital,
  });
});

// 2. Standalone Doctor Code verification route (isolated controller for step 2)
router.post('/verifyDoctorCode', authmiddleware,verifyDoctorCode, (req, res) => {
  return res.status(200).json({
    success: true,
    message: 'Doctor verified successfully!',
    data: req.authData, // Contains final generated JWT and doctor details
  });
});
// -------------------------------------------------------------
// 2. DYNAMIC / PARAMETERIZED ROUTES (Place at the bottom)
// -------------------------------------------------------------
// GET /api/v2/hospitals/:hospitalId/departments/:departmentName/doctors
router.get('/:hospitalId/departments/:departmentName/doctors', getDoctorsByDepartment);
// Get department list for a hospital
router.get('/:hospitalId/getDepartments', getHospitalDepartments);

// Add departments to a hospital
router.put('/:hospitalId/addDepartments', addDepartmentsToHospital);

module.exports = router;
const express = require('express');
const router = express.Router();

// 1. Destructure named exports cleanly
const { 
  registerUser, 
  createStaffUser, 
  loginUser, 
  registerDoctor
} = require('../new_controllers/auth_controllers');

// 2. Import middleware functions
const { authmiddleware, authorize } = require('../new_middleware/authmiddleware');
console.log('--- DEBUG IMPORTS ---');
console.log('authmiddleware type:', typeof authmiddleware);
console.log('authorize type:', typeof authorize);
console.log('registerUser type:', typeof registerUser); // <-- Change auth_controllers.registerUser to registerUser
console.log('---------------------');
// 3. Public Routes (DO NOT put authmiddleware here)
router.post('/register', registerUser);
router.post('/login', loginUser);

// 4. Protected Routes (Must invoke authorize as a function with 'SUPER_ADMIN')
router.post(
  '/createCompounder', 
  authmiddleware, 
  authorize('SUPER_ADMIN'), 
  createStaffUser
);
router.post(
  '/registerDoctor', 
  authmiddleware, 
  authorize('SUPER_ADMIN'), 
  registerDoctor
);
module.exports = router;
const express = require('express');
const router = express.Router();

// 1. Destructure named exports cleanly from your admin/dashboard controller
const { 
    getAdminDashboardData, 
   getDoctorDirectory
} = require('../new_controllers/admin_controllers');

// 2. Import middleware functions
const { authmiddleware, authorize } = require('../new_middleware/authmiddleware');

console.log('--- DEBUG ADMIN IMPORTS ---');
console.log('authmiddleware type:', typeof authmiddleware);
console.log('authorize type:', typeof authorize);
console.log('getAdminDashboardStats type:', typeof getAdminDashboardStats);
console.log('sendBroadcastAnnouncement type:', typeof sendBroadcastAnnouncement);
console.log('---------------------------');

// 3. Protected Admin Routes (Requires authentication and appropriate admin roles)
router.post(
    '/dashboardStats', 
    authmiddleware, 
    authorize('SUPER_ADMIN', 'ADMIN'), 
    getAdminDashboardData
);
router.post(
'/doctors-directory',
authmiddleware,
authorize('SUPER_ADMIN', 'ADMIN'), 
getDoctorDirectory
)

module.exports = router;
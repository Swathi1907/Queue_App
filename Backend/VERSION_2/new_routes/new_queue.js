const express = require('express');
const router = express.Router();
const { authmiddleware, authorize } = require('../new_middleware/authmiddleware'); 

const QueueV2 = require('../new_models/new_queuev2');
const UserV2 = require('../new_models/peron_model');

const { createDepartmentQueue } = require('../new_controllers/queue_controllers');


// POST route to create a department queue (protected by authentication middleware)
console.log('--- DEBUG IMPORTS ---');

console.log('createDepartmentQueue:', typeof createDepartmentQueue, createDepartmentQueue);
console.log('---------------------');


router.post('/createDepartmentQueue', authmiddleware, authorize('DOCTOR'),createDepartmentQueue);

module.exports = router;
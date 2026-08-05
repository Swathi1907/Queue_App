const express = require('express');
const router = express.Router();
const { authmiddleware, authorize } = require('../new_middleware/authmiddleware'); 

const QueueV2 = require('../new_models/new_queuev2');
const UserV2 = require('../new_models/peron_model');

const { getDoctorProfile,session_there,next,completeCurrent} = require('../new_controllers/docotr_controller');




router.get('/getDoctorProfile', authmiddleware,getDoctorProfile);
router.get('/session_there',authmiddleware,session_there);
router.post('/queue/next',authmiddleware,next);
router.post('/queue/completeCurrent',authmiddleware,completeCurrent)
module.exports = router;
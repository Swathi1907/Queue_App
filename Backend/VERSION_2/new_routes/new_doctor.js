const express = require('express');
const router = express.Router();
const { authmiddleware, authorize } = require('../new_middleware/authmiddleware'); 

const QueueV2 = require('../new_models/new_queuev2');
const UserV2 = require('../new_models/peron_model');

const { getOverviewStats,getOverviewDepartments,getDoctorProfile,session_there,next,completeCurrent,updateQueueStatus,getDoctorAnalytics,end_session, getConsultationActivity} = require('../new_controllers/docotr_controller');


router.get('/getConsultationActivity',authmiddleware,getConsultationActivity)
router.get('/OverviewDepartments',authmiddleware,getOverviewDepartments)
router.get('/getDoctorProfile', authmiddleware,getDoctorProfile);
router.post('/updateQueueStatus',authmiddleware,updateQueueStatus);
router.get('/session_there',authmiddleware,session_there);
router.post('/queue/next',authmiddleware,next);
router.post('/queue/completeCurrent',authmiddleware,completeCurrent)
router.post('/end_session',authmiddleware,end_session)
router.get('/doctorAnalytics',getDoctorAnalytics)
router.get('/OverviewStats',authmiddleware,getOverviewStats)
module.exports = router;
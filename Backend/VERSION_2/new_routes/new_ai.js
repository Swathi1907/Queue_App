const express = require('express');
const router = express.Router();
const multer = require('multer');

const { scanDoctorResume } = require('../new_controllers/ai_control');
const { authmiddleware, authorize } = require('../new_middleware/authmiddleware');

// Configure multer memory storage for file uploads
const upload = multer({ 
    limits: { fileSize: 10 * 1024 * 1024 }, // 10MB limit
    fileFilter: (req, file, cb) => {
        if (file.mimetype === 'application/pdf' || file.mimetype.includes('document')) {
            cb(null, true);
        } else {
            cb(new Error('Only PDF and DOCX files are allowed!'), false);
        }
    }
});

router.post(
    '/scanDoctorResume', 
    authmiddleware, 
    authorize('SUPER_ADMIN', 'ADMIN'), 
    upload.single('resume'), // Expecting field name 'resume' from multipart form data
    scanDoctorResume
);

module.exports = router;
const express = require("express");
const route = express.Router();
const Queue = require('../models/Queue.models');
const Hospital = require("../models/hospital.models");
const Doctor=require('../models/doctor_model');
// Create Hospital
route.post("/create", async (req, res) => {

    try {

        const { hospitalName, address, city } = req.body;

        if (!hospitalName || !address || !city) {
            return res.status(400).json({
                message: "All fields are required"
            });
        }

        // Check duplicate hospital name
        const existingHospital = await Hospital.findOne({
            hospitalName,
            address
        });

        if (existingHospital) {
            return res.status(400).json({
                message: "Hospital already exists"
            });
        }

        // Generate Hospital ID
        const hospitalCount = await Hospital.countDocuments();

       // const hospitalId = `HOSP-${1001 + hospitalCount}`;
const hospitalId = "HOSP-" + Math.floor(100000 + Math.random() * 900000);
        const hospital = new Hospital({

            hospitalName,
            hospitalId,
            address,
            city,
            
        });

        await hospital.save();

        res.status(201).json({

            message: "Hospital created successfully",

            hospital

        });

    } catch (err) {

        res.status(500).json({
            message: err.message
        });

    }

});

route.get("/:hospitalId/doctors", async (req, res) => {

    try {

        const hospital = await Hospital.findOne({
            hospitalId: req.params.hospitalId
        });

        if (!hospital) {
            return res.status(404).json({
                message: "Hospital not found"
            });
        }

        const doctors = await Doctor.find({
            hospitalcode: req.params.hospitalId
        }).sort({ doctorName: 1 });

        res.json({
            hospital,
            doctors
        });

    } catch (err) {

        res.status(500).json({
            message: err.message
        });

    }

});



route.post("/doctor/create", async (req, res) => {

    try {

        const {
            hospitalcode,
            doctorName,
            specialization,
            qualification,
            roomNumber,
            availableDays,
            startTime,
            endTime
        } = req.body;

        if (
            !hospitalcode ||
            !doctorName ||
            !specialization ||
            !availableDays ||
            !startTime ||
            !endTime
        ) {
            return res.status(400).json({
                message: "All required fields are mandatory"
            });
        }

        // Check whether hospital exists
        const hospital = await Hospital.findOne({
            hospitalId: hospitalcode
        });

        if (!hospital) {
            return res.status(404).json({
                message: "Hospital not found"
            });
        }

        const doctor = new Doctor({

            hospitalcode,
            doctorName,
            specialization,
            qualification,
            roomNumber,
            availableDays,
            startTime,
            endTime

        });

        await doctor.save();

        res.status(201).json({

            message: "Doctor added successfully",
            doctor

        });

    } catch (err) {

        res.status(500).json({
            message: err.message
        });

    }

});



// Verify Hospital ID
route.post("/verify", async (req, res) => {

    try {

        const { hospitalId } = req.body;

        const hospital = await Hospital.findOne({ hospitalId });

        if (!hospital) {
            return res.status(404).json({
                valid: false,
                message: "Invalid Hospital ID"
            });
        }

        res.status(200).json({
            valid: true,
            hospitalId: hospital.hospitalId,
            hospitalName: hospital.hospitalName
        });

    } catch (err) {

        res.status(500).json({
            message: err.message
        });
    }

});


// Get All Hospitals

route.get("/allhospitals", async (req, res) => {
console.log("allapihit")
    try {

        const queues = await Queue.find({
            queueStatus: { $in: ["active", "paused"] }
        });
console.log("Queues:", queues);


const hospitalIds = [...new Set(queues.map(q => q.hospitalId))];
console.log("Hospital IDs:", hospitalIds);

const hospitals = await Hospital.find({
    hospitalId: { $in: hospitalIds }
});

console.log("Hospitals:", hospitals);
      

       

        res.json(hospitals);

    } catch (err) {

        res.status(500).json({
            message: err.message
        });

    }

});
module.exports = route;
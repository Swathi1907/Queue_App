package com.swathi.queue_app.model

data class DoctorRequest(
    val hospitalcode: String,
    val doctorName: String,
    val specialization: String,
    val qualification: String,
    val roomNumber: String,
    val availableDays: List<String>,
    val startTime: String,
    val endTime: String
)
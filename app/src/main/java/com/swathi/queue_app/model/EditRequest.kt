package com.swathi.queue_app.model
//doctorName,
//specialization,
//qualification,
//roomNumber,
//availableDays,
//startTime,
//endTime
data class EditRequest(
    val doctorName: String,
    val specialization: String,
    val qualification: String,
    val roomNumber: Int,
    val availableDays: List<String>,
    val startTime: String,
    val endTime: String
)
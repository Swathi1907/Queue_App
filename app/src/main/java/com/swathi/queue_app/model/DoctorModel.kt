package com.swathi.queue_app.model

import com.google.gson.annotations.SerializedName

data class DoctorModel(

    @SerializedName("_id")
    val id: String,

    val hospitalcode: String,

    val doctorName: String,

    val specialization: String,

    val qualification: String,

    val roomNumber: String,

    val availableDays: List<String>,

    val startTime: String,

    val endTime: String,
    val createdAt: String,

    val updatedAt: String
)
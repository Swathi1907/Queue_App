package com.swathi.queue_app.model

data class DoctorListResponse(
    val hospital: HospitalModel,
    val doctors: List<DoctorModel>
)
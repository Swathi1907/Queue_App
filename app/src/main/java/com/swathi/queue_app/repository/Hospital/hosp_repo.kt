package com.swathi.queue_app.repository.admin
import android.content.Context
import androidx.core.content.ContentProviderCompat.requireContext
import com.swathi.queue_app.api.RetrofitInstance
import com.swathi.queue_app.model.DoctorRequest
import com.swathi.queue_app.model.EditRequest


class hosp_repo {


    suspend fun addDoctor(request: DoctorRequest) =
      RetrofitInstance.api.addDoctor(request)

    suspend fun getDoctors(hospitalId: String) =
        RetrofitInstance.api.getDoctors(hospitalId)

    suspend fun updateDoctor(doctorId :String,request: EditRequest)=
        RetrofitInstance.api.updateDoctor(doctorId, request)

}
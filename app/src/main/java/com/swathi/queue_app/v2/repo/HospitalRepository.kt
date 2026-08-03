package com.swathi.queue_app.v2.repo



import com.swathi.queue_app.v2.models.CreateQueueRequest
import com.swathi.queue_app.v2.models.DepartmentResponse
import com.swathi.queue_app.v2.models.DoctorResponse
import com.swathi.queue_app.v2.models.Hospital
import com.swathi.queue_app.v2.network.RetrofitInstance
import com.swathi.queue_app.v2.models.LoginRequest
import com.swathi.queue_app.v2.models.SignupRequest
import com.swathi.queue_app.v2.models.VerifyHospitalRequest

class HospitalRepository {
    suspend fun getDepartments(hospitalId: String)
     =RetrofitInstance.api.getDepartments(hospitalId)


    suspend fun getDoctors(hospitalId: String, department: String)=

             RetrofitInstance.api.getDoctorsByDepartment(hospitalId, department)

    suspend fun fetchHospitals(): Result<List<Hospital>> {
        return try {
            val response = RetrofitInstance.api.getAllHospitals()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.data)
            } else {
                Result.failure(Exception("Failed to load hospitals: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
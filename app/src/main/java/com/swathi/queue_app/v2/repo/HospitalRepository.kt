package com.swathi.queue_app.v2.repo

import com.swathi.queue_app.v2.models.Hospital
import com.swathi.queue_app.v2.models.HospitalDetailResponse
import com.swathi.queue_app.v2.models.UserDoctorResponse
import com.swathi.queue_app.v2.network.RetrofitInstance
import retrofit2.Response

class HospitalRepository {

    suspend fun getDepartments(hospitalId: String) =
        RetrofitInstance.api.getDepartments(hospitalId).also {
            println("Through getDepartments HospitalRepository called")
        }

    suspend fun getDoctors(hospitalId: String, department: String) =
        RetrofitInstance.api.getDoctorsByDepartment(hospitalId, department).also {
            println("Through getDoctors HospitalRepository called")
        }

    suspend fun fetchHospitals(): Result<List<Hospital>> {
        return try {
            val response = RetrofitInstance.api.getAllHospitals().also {
                println("Through fetchHospitals HospitalRepository called")
            }
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.data)
            } else {
                Result.failure(Exception("Failed to load hospitals: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserDoctors(hospitalId: String, department: String): Response<UserDoctorResponse>? {
        return try {
            RetrofitInstance.api.getUserDoctors(hospitalId, department).also {
                println("Through getUserDoctors HospitalRepository called")
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getHospitalDetails(hospitalId: String): Result<HospitalDetailResponse> {
        return try {
            val response = RetrofitInstance.api.getHospitalById(hospitalId).also {
                println("Through getHospitalDetails HospitalRepository called")
            }
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to load hospital details"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
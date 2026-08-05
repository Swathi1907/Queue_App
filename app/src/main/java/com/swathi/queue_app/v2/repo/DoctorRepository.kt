package com.swathi.queue_app.v2.repo

import com.swathi.queue_app.v2.models.DoctorProfileResponse
import com.swathi.queue_app.v2.network.ApiService
import com.swathi.queue_app.v2.network.RetrofitInstance

class DoctorRepository(
    private val apiService: ApiService = RetrofitInstance.api
) {

    suspend fun getDoctorProfile(): Result<DoctorProfileResponse> {
        return try {
            val response = apiService.getDoctorProfile()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error: ${response.errorBody()?.string() ?: response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
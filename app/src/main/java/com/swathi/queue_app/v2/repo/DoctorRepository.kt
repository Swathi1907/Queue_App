package com.swathi.queue_app.v2.repo

import com.swathi.queue_app.v2.models.DoctorProfileResponse
import com.swathi.queue_app.v2.models.EndSessionResponse
import com.swathi.queue_app.v2.models.ResumeScanResponse
import com.swathi.queue_app.v2.network.ApiService
import com.swathi.queue_app.v2.network.RetrofitInstance
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody

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


    suspend fun endSession(
        department: String?,
        doctorCode: String?
    ): Result<EndSessionResponse> {

        return try {
            val response = apiService.end_session(
                department,
                doctorCode
            )

            if (response.isSuccessful && response.body() != null) {

                Result.success(response.body()!!)

            } else {

                Result.failure(
                    Exception(
                        response.errorBody()?.string()
                            ?: "Failed to end session"
                    )
                )
            }

        } catch (e: Exception) {

            Result.failure(e)
        }
    }
    // New AI Resume Scan Repository Method
    suspend fun scanDoctorResume(
        hospitalId: String,
        resumePart: MultipartBody.Part
    ): Result<ResumeScanResponse> {
        return try {
            // Send hospitalId as a simple string part to match your backend's req.body requirement
            val hospitalIdPart = RequestBody.create("text/plain".toMediaTypeOrNull(), hospitalId)

            val response = apiService.scanDoctorResume(hospitalIdPart, resumePart)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                // Parse the error body if it exists (e.g., "No matching department found...")
                val errorMsg = response.errorBody()?.string() ?: response.message()
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
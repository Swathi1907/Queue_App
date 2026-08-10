package com.swathi.queue_app.v2.repo

import android.content.Context
import com.swathi.queue_app.v2.models.AdminDashboardData
import com.swathi.queue_app.v2.models.AdminDashboardResponse
import com.swathi.queue_app.v2.models.DoctorDirectoryResponse
import com.swathi.queue_app.v2.models.GlobalQueueResponseWrapper
import com.swathi.queue_app.v2.models.HospitalIdRequest
import com.swathi.queue_app.v2.network.ApiService
import com.swathi.queue_app.v2.network.RetrofitInstance
import com.swathi.queue_app.v2.utilis.TokenManager

class AdminRepository(
    private val context: Context,
    private val apiService: ApiService = RetrofitInstance.api
) {
    private val tokenManager = TokenManager(context)

    suspend fun getDashboardStats(): Result<AdminDashboardData> {
        return try {
            val hospitalId = tokenManager.getHospitalId() ?: ""
            if (hospitalId.isEmpty()) {
                return Result.failure(Exception("Hospital ID is missing"))
            }

            val request = HospitalIdRequest(hospitalId = hospitalId)
            val response = apiService.getAdminDashboardStats(request)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success) {
                    Result.success(body.data)
                } else {
                    Result.failure(Exception(body.message))
                }
            } else {
                Result.failure(Exception("Error: ${response.errorBody()?.string() ?: response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDoctorDirectory(hospitalId: String): Result<DoctorDirectoryResponse> {
        return try {
            val request = HospitalIdRequest(hospitalId)
            // Fixed: Call the correct doctor directory endpoint
            val response = apiService.getDoctorDirectory(request)

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Response body is null"))
                }
            } else {
                Result.failure(Exception("Error: ${response.errorBody()?.string() ?: response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getGlobalDepartments(): Result<GlobalQueueResponseWrapper> {
        return try {
            val hospitalId = tokenManager.getHospitalId() ?: ""
            if (hospitalId.isEmpty()) {
                return Result.failure(Exception("Hospital ID is missing"))
            }

            val request = HospitalIdRequest(hospitalId = hospitalId)
            val response = apiService.getGlobalDepartments(request)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Failed to load global queues"))
                }
            } else {
                Result.failure(Exception("Error: ${response.errorBody()?.string() ?: response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
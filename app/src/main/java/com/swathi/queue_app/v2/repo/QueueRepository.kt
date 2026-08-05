package com.swathi.queue_app.v2.repo



import com.swathi.queue_app.v2.models.ActiveSessionResponse
import com.swathi.queue_app.v2.models.CreateQueueRequest
import com.swathi.queue_app.v2.models.DashboardData
import com.swathi.queue_app.v2.network.RetrofitInstance
import com.swathi.queue_app.v2.models.LoginRequest
import com.swathi.queue_app.v2.models.QueueActionRequest
import com.swathi.queue_app.v2.models.QueueActionResponse
import com.swathi.queue_app.v2.models.SignupRequest
import com.swathi.queue_app.v2.models.VerifyHospitalRequest
import retrofit2.Response

class QueueRepository {


    suspend fun createQueue(request: CreateQueueRequest) =
        RetrofitInstance.api.createDepartmentQueue(request).also{
            println("Repository called from create queue")
        }
    suspend fun getActiveSession(department: String, doctorCode: String): Response<ActiveSessionResponse> {
        return RetrofitInstance.api.getActiveSession(department, doctorCode)
    }
    suspend fun fetchDashboard(userId: String): Result<DashboardData?> {
        return try {
            val response = RetrofitInstance.api.getUserDashboard(userId)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()?.data)
            } else {
                Result.failure(Exception("Failed to load queue dashboard"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun callNextPatient(department: String, doctorCode: String): Response<QueueActionResponse> {
        return RetrofitInstance.api.callNextPatient(QueueActionRequest(department, doctorCode))
    }

    suspend fun completeCurrent(department: String, doctorCode: String): Response<QueueActionResponse> {
        return RetrofitInstance.api.completeConsultation(QueueActionRequest(department, doctorCode))
    }

}
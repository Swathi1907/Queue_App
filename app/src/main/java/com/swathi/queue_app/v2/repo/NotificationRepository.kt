package com.swathi.queue_app.v2.repo

import com.swathi.queue_app.v2.models.NotificationModel
import com.swathi.queue_app.v2.network.ApiService
import com.swathi.queue_app.v2.network.RetrofitInstance

class NotificationRepository(
    private val apiService: ApiService = RetrofitInstance.api
) {

    suspend fun getNotifications(
        targetRole: String
    ): Result<List<NotificationModel>> {

        return try {

            val response = apiService.getNotifications(
                targetRole
            )

            if (response.isSuccessful && response.body() != null) {

                Result.success(response.body()!!)

            } else {

                Result.failure(
                    Exception(
                        response.errorBody()?.string()
                            ?: "Failed to fetch notifications"
                    )
                )
            }

        } catch (e: Exception) {

            Result.failure(e)
        }
    }
}
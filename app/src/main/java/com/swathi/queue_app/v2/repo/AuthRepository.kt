package com.swathi.queue_app.v2.repo

import com.swathi.queue_app.v2.network.RetrofitInstance
import com.swathi.queue_app.v2.models.LoginRequest
import com.swathi.queue_app.v2.models.SignupRequest

class AuthRepository {

    suspend fun login(request: LoginRequest) =
        RetrofitInstance.api.loginUser(
            request.also {
                println("Repository login called")
            }
        )

    suspend fun signup(request: SignupRequest) =
        RetrofitInstance.api.registerUser(
            request.also {
                println("Repository signup called")
            }
        )

    suspend fun saveFcmToken(token: String) =
        RetrofitInstance.api.saveFcmToken(
            mapOf("fcmToken" to token)
        )

    suspend fun getProfile() =
        RetrofitInstance.api.getProfile()

    suspend fun verifyHospital(hospitalId: String) =
        RetrofitInstance.api.verifyHospital(
            mapOf("hospitalId" to hospitalId)
        )

    suspend fun getAllHospitals() =
        RetrofitInstance.api.getAllHospitals()
}
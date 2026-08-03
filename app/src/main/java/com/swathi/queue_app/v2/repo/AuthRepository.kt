package com.swathi.queue_app.v2.repo

import com.swathi.queue_app.v2.network.RetrofitInstance
import com.swathi.queue_app.v2.models.LoginRequest
import com.swathi.queue_app.v2.models.SignupRequest
import com.swathi.queue_app.v2.models.VerifyDoctorCodeRequest
import com.swathi.queue_app.v2.models.VerifyHospitalRequest

class AuthRepository {

    suspend fun login(request: LoginRequest) =
        RetrofitInstance.api.loginUser(request).also{
            println("Repository called")
        }



    suspend fun signup(request: SignupRequest) =
        RetrofitInstance.api.registerUser(
           request
        ).also{
            println(" through sign up Repository called")
        }

    suspend fun verifyHospitalId(request: VerifyHospitalRequest)
    =RetrofitInstance.api.verifyHospitalId(request).also{
        println(" through verify hospital Repository called")
    }
    suspend fun verifyDoctor(request: VerifyDoctorCodeRequest)
            =RetrofitInstance.api.verifyDoctor(request).also{
        println(" through verify doctor Repository called")
    }

}
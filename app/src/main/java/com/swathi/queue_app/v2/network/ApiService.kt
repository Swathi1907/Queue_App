package com.swathi.queue_app.v2.network

import com.swathi.queue_app.v2.models.AuthResponse
import com.swathi.queue_app.v2.models.CreateQueueRequest
import com.swathi.queue_app.v2.models.DepartmentResponse
import com.swathi.queue_app.v2.models.DepartmentResponseWrapper
import com.swathi.queue_app.v2.models.DoctorResponse
import com.swathi.queue_app.v2.models.HospitalResponse
import com.swathi.queue_app.v2.models.LoginRequest
import com.swathi.queue_app.v2.models.QueueResponseWrapper
import com.swathi.queue_app.v2.models.SignupRequest
import com.swathi.queue_app.v2.models.VerifyDoctorCodeRequest
import com.swathi.queue_app.v2.models.VerifyDoctorCodeResponse
import com.swathi.queue_app.v2.models.VerifyHospitalRequest
import com.swathi.queue_app.v2.models.VerifyHospitalResponse
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {

    @POST("api/v2/auth/register") // Adjust path based on your backend routes
    suspend fun registerUser(@Body request: SignupRequest): Response<AuthResponse>

    @POST("api/v2/hospital/verifyHospitalId") // Update this route to match your actual backend route
    suspend fun verifyHospitalId(
        @Body request: VerifyHospitalRequest
    ): Response<VerifyHospitalResponse>

    @POST("api/v2/hospital/verifyDoctorCode")
    suspend fun verifyDoctor(
        @Body request: VerifyDoctorCodeRequest
    ): Response<VerifyDoctorCodeResponse>
    @POST("api/v2/auth/login")
    suspend fun loginUser(@Body request: LoginRequest): Response<AuthResponse>

    @POST("api/v2/queue/createDepartmentQueue") // Matches your Express route
    suspend fun createDepartmentQueue(
        @Body request: CreateQueueRequest
    ): Response<QueueResponseWrapper>

    @GET("api/v2/hospital/getAllHospitals") // Update with your exact route path if needed
    suspend fun getAllHospitals(): Response<HospitalResponse>

    @GET("api/v2/hospital/{hospitalId}/getDepartments")
   suspend  fun getDepartments(
        @Path("hospitalId") hospitalId: String
    ): Response<DepartmentResponseWrapper>

    @GET("api/v2/hospital/{hospitalId}/departments/{departmentName}/doctors")
    suspend fun getDoctorsByDepartment(
        @Path("hospitalId") hospitalId: String,
        @Path("departmentName") departmentName: String
    ): Response<DoctorResponse>
}
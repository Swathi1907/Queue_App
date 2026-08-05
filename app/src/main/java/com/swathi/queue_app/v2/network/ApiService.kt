package com.swathi.queue_app.v2.network

import com.swathi.queue_app.v2.models.ActiveSessionResponse
import com.swathi.queue_app.v2.models.AuthResponse
import com.swathi.queue_app.v2.models.CreateQueueRequest
import com.swathi.queue_app.v2.models.DepartmentResponse
import com.swathi.queue_app.v2.models.DepartmentResponseWrapper
import com.swathi.queue_app.v2.models.DoctorProfileResponse
import com.swathi.queue_app.v2.models.DoctorResponse
import com.swathi.queue_app.v2.models.HospitalDetailResponse
import com.swathi.queue_app.v2.models.HospitalResponse
import com.swathi.queue_app.v2.models.LoginRequest
import com.swathi.queue_app.v2.models.OrderCreateRequest
import com.swathi.queue_app.v2.models.OrderCreateResponse
import com.swathi.queue_app.v2.models.PaymentVerifyRequest
import com.swathi.queue_app.v2.models.PaymentVerifyResponse
import com.swathi.queue_app.v2.models.QueueActionRequest
import com.swathi.queue_app.v2.models.QueueActionResponse
import com.swathi.queue_app.v2.models.QueueDashboardResponse
import com.swathi.queue_app.v2.models.QueueResponseWrapper
import com.swathi.queue_app.v2.models.SignupRequest
import com.swathi.queue_app.v2.models.UserDoctorResponse
import com.swathi.queue_app.v2.models.VerifyDoctorCodeRequest
import com.swathi.queue_app.v2.models.VerifyDoctorCodeResponse
import com.swathi.queue_app.v2.models.VerifyHospitalRequest
import com.swathi.queue_app.v2.models.VerifyHospitalResponse
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @POST("api/v2/auth/register") // Adjust path based on your backend routes
    suspend fun registerUser(@Body request: SignupRequest): Response<AuthResponse>

    @GET("api/v2/doctor/getDoctorProfile")
    suspend fun getDoctorProfile(): Response<DoctorProfileResponse>

    @POST("api/v2/hospital/verifyHospitalId") // Update this route to match your actual backend route
    suspend fun verifyHospitalId(@Header("Authorization") token: String,
                                 @Body request: VerifyHospitalRequest
    ): Response<VerifyHospitalResponse>

    @POST("/api/v2/doctor/queue/next")
    suspend fun callNextPatient(
        @Body request: QueueActionRequest
    ): Response<QueueActionResponse>

    @POST("/api/v2/doctor/queue/completeCurrent")
    suspend fun completeConsultation(
        @Body request: QueueActionRequest
    ): Response<QueueActionResponse>


    @GET("api/v2/doctor/session_there")
    suspend fun getActiveSession(
        @Query("department") department: String,
        @Query("doctorCode") doctorCode: String
    ): Response<ActiveSessionResponse>

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
    @POST("api/v2/payment/create-order")
    suspend fun createRazorpayOrder(
        @Body request: OrderCreateRequest
    ): Response<OrderCreateResponse>
    @GET("api/v2/hospital/getAllHospitals") // Update with your exact route path if needed
    suspend fun getAllHospitals(): Response<HospitalResponse>
    @GET("api/v2/hospital/{hospitalId}/getHospital") // Match your exact Express route path
    suspend fun getHospitalById(
        @Path("hospitalId") hospitalId: String
    ): Response<HospitalDetailResponse>
    @GET("api/v2/hospital/{hospitalId}/getDepartments")
   suspend  fun getDepartments(
        @Path("hospitalId") hospitalId: String
    ): Response<DepartmentResponseWrapper>

    @POST("api/v2/payment/verify")
    suspend fun verifyPayment(
        @Body request: PaymentVerifyRequest
    ): Response<PaymentVerifyResponse>
    @GET("api/v2/hospital/{hospitalId}/departments/{departmentName}/Usersidedoctors")
    suspend fun getUserDoctors(
        @Path("hospitalId") hospitalId: String,
        @Path("departmentName") departmentName: String,
        @Query("userId") currentUserId: String
    ): Response<UserDoctorResponse>
    @GET("api/v2/hospital/{hospitalId}/departments/{departmentName}/doctors")
    suspend fun getDoctorsByDepartment(
        @Path("hospitalId") hospitalId: String,
        @Path("departmentName") departmentName: String
    ): Response<DoctorResponse>

    @GET("api/v2/queue/getUserQueues/{userId}")
    suspend fun getUserDashboard(
        @Path("userId") userId: String
    ): Response<QueueDashboardResponse>

}
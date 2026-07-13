package com.swathi.queue_app.api

import com.swathi.queue_app.model.ActiveQueueResponse
import com.swathi.queue_app.model.CompleteCurrentResponse
import com.swathi.queue_app.model.CreateQueueRequest
import com.swathi.queue_app.model.HospitalModel
import com.swathi.queue_app.model.Loginrequest
import com.swathi.queue_app.model.Loginresponse
import com.swathi.queue_app.model.MemberModel
import com.swathi.queue_app.model.MessageResponse
import com.swathi.queue_app.model.NextResponse
import com.swathi.queue_app.model.NotificationCountResponse
import com.swathi.queue_app.model.NotificationModel
import com.swathi.queue_app.model.NotificationReadResponse
import com.swathi.queue_app.model.ProfileResponse
import com.swathi.queue_app.model.QueueDetailsResponse
import com.swathi.queue_app.model.QueueModel
import com.swathi.queue_app.model.QueueStatusResponse
import com.swathi.queue_app.model.VerifyHospitalResponse
import com.swathi.queue_app.model.adminDashboardresponse
import com.swathi.queue_app.model.adminactivequeues
import com.swathi.queue_app.model.joinQueueResponse
import com.swathi.queue_app.model.myStatusResponse
import com.swathi.queue_app.model.signuprequest
import com.swathi.queue_app.model.signupresponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface Apiservice{
    @POST("api/auth/login")
    suspend fun login(
        @Body request: Loginrequest
    ): Response<Loginresponse>

  //  @GET("api/queue/allQueues")
    //suspend fun getAllQueues(): Response<List<QueueModel>>
  @GET("api/queue/allQueues")
  suspend fun getAllQueues(
      @Query("hospitalId") hospitalId: String
  ): Response<List<QueueModel>>
    @POST("api/auth/signup")
    suspend fun signup(
        @Body request: signuprequest
    ):Response<signupresponse>

    @POST("api/queue/{queueId}/join")
    suspend fun joinQueue(
        @Path("queueId") queueId: String
    ):Response<joinQueueResponse>

@GET("api/queue/{queueId}/myStatus")
suspend fun myStatus(
    @Path("queueId") queueId: String?
) :Response<myStatusResponse>


    @GET("api/queue/myActiveQueue")
    suspend fun getMyActiveQueue():
            Response<ActiveQueueResponse>

    @POST("api/queue/{queueId}/exit")
    suspend fun exitQueue(
        @Path("queueId") queueId: String
    ): Response<MessageResponse>

    @GET("api/admin/dashboard")
    suspend fun dashboard(
        @Query("hospitalId") hospitalId: String
    ):Response<adminDashboardresponse>

    @POST("api/queue/{queueId}/next")
    suspend fun nextToken(
        @Path("queueId") queueId: String
    ):Response<NextResponse>

    @POST("api/queue/{queueId}/toggleQueueStatus")
    suspend fun toggleQueueStatus(
        @Path("queueId") queueId: String
    ): Response<QueueStatusResponse>


    @POST("api/queue/{queueId}/close")
    suspend fun closeQueue(
        @Path("queueId") queueId: String
    ): Response<MessageResponse>

    @GET("api/queue/{queueId}/allMembers")
    suspend fun getAllMembers(
        @Path("queueId") queueId: String
    ): Response<List<MemberModel>>

    @GET("api/queue/{queueId}/details")
    suspend fun getQueueDetails(
        @Path("queueId") queueId: String?
    ): Response<QueueDetailsResponse>

    @POST("api/queue/create")
    suspend fun createQueue(
        @Body request: CreateQueueRequest
    ): Response<MessageResponse>

    @GET("api/queue/{queueId}/userMembers")
    suspend fun getUserMembers(
        @Path("queueId") queueId: String?
    ): Response<List<MemberModel>>



    @GET("api/queue/admin/activequeues")
    suspend fun getadminActiveQueues(
        @Query("hospitalId") hospitalId: String
    ): Response<List<adminactivequeues>>

    @GET("api/auth/profile")
    suspend fun getProfile():
            Response<ProfileResponse>
    @POST("api/queue/{queueId}/completeCurrent")
    suspend fun completeCurrent(
        @Path("queueId") queueId: String
    ): Response<CompleteCurrentResponse>

    @POST("api/auth/saveFcmToken")
    suspend fun saveFcmToken(
        @Body body: Map<String, String>
    ): Response<MessageResponse>


    @GET("api/notification")
    suspend fun getNotifications():
            Response<List<NotificationModel>>

    @GET("api/notification/count")
    suspend fun getNotificationCount():
            Response<NotificationCountResponse>

    @PUT("api/notification/read")
    suspend fun markNotificationsRead():
            Response<NotificationReadResponse>


    @POST("api/hospital/verify")
    suspend fun verifyHospital(
        @Body body: Map<String, String>
    ): Response<VerifyHospitalResponse>

    @GET("api/hospital/allHospitals")
    suspend fun getAllHospitals():
            Response<List<HospitalModel>>
}
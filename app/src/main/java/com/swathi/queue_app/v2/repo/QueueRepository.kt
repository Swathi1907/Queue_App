package com.swathi.queue_app.v2.repo



import com.swathi.queue_app.v2.models.CreateQueueRequest
import com.swathi.queue_app.v2.network.RetrofitInstance
import com.swathi.queue_app.v2.models.LoginRequest
import com.swathi.queue_app.v2.models.SignupRequest
import com.swathi.queue_app.v2.models.VerifyHospitalRequest

class QueueRepository {


    suspend fun createQueue(request: CreateQueueRequest) =
        RetrofitInstance.api.createDepartmentQueue(request).also{
            println("Repository called from create queue")
        }
}
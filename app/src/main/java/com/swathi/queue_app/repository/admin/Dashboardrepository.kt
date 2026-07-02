package com.swathi.queue_app.repository.admin
import com.swathi.queue_app.api.RetrofitInstance
class Dashboardrepository {
    suspend fun dashboard() =
        RetrofitInstance.api.dashboard()
    suspend fun getActiveQueues() =
        RetrofitInstance.api.getadminActiveQueues()
}
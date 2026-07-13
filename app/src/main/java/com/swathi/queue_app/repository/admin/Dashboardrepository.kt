package com.swathi.queue_app.repository.admin
import android.content.Context
import androidx.core.content.ContentProviderCompat.requireContext
import com.swathi.queue_app.api.RetrofitInstance
class Dashboardrepository {
    suspend fun dashboard(hospitalId: String) =
        RetrofitInstance.api.dashboard(hospitalId)


    suspend fun getActiveQueues(hospitalId: String) =
        RetrofitInstance.api.getadminActiveQueues(hospitalId)
}
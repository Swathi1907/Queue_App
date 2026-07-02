package com.swathi.queue_app.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swathi.queue_app.model.ActiveQueueResponse
import com.swathi.queue_app.model.NotificationCountResponse
import com.swathi.queue_app.model.NotificationModel
import com.swathi.queue_app.model.NotificationReadResponse
import com.swathi.queue_app.model.QueueModel
import com.swathi.queue_app.repository.Queuerepository
import kotlinx.coroutines.launch
class HomeViewModel : ViewModel(){
    private val Queuerepository= Queuerepository();
    val allqueueResponse= MutableLiveData<List<QueueModel>>()
    fun getAllQueues(){
        try {
            viewModelScope.launch {
            val response = Queuerepository.getAllQueues()

                println(response.code())

                println(response.isSuccessful)
                println(response.errorBody()?.string())
                println(response.body())
                if(response.isSuccessful){
                   response.body()?.let{
allqueueResponse.value=it
                }
                }
        }

        }
        catch(e: Exception){
println("Error: ${e.message}")
            e.printStackTrace()
        }
    }
    private val _notificationCount = MutableLiveData<NotificationCountResponse>()
    val notificationCount: LiveData<NotificationCountResponse> = _notificationCount
    fun getNotificationCount() {

        viewModelScope.launch {

            val response = Queuerepository.getNotificationCount()

            if (response.isSuccessful && response.body() != null) {

                _notificationCount.value = response.body()
            }
        }
    }
    private val _readNotificationResponse = MutableLiveData<NotificationReadResponse>()
    val readNotificationResponse: LiveData<NotificationReadResponse> = _readNotificationResponse

    fun markNotificationsRead() {

        viewModelScope.launch {

            val response = Queuerepository.markNotificationsRead()

            if (response.isSuccessful && response.body() != null) {

                _readNotificationResponse.value = response.body()
            }
        }
    }
    val activeQueueResponse =
        MutableLiveData<ActiveQueueResponse>()

    fun getMyActiveQueue() {

        viewModelScope.launch {

            val response =
                Queuerepository.getMyActiveQueue()

            if(response.isSuccessful){

                response.body()?.let {

                    activeQueueResponse.value = it
                }
            }
        }
    }
    val notifications = MutableLiveData<List<NotificationModel>>()

    fun getNotifications() {

        viewModelScope.launch {

            try {

                val response = Queuerepository.getNotifications()

                Log.d("NOTIFICATION", "Code = ${response.code()}")
                Log.d("NOTIFICATION", "Successful = ${response.isSuccessful}")
                Log.d("NOTIFICATION", "Body = ${response.body()}")

                if (response.isSuccessful && response.body() != null) {

                    notifications.value = response.body()

                    Log.d("NOTIFICATION", "LiveData Updated")

                }

            } catch (e: Exception) {

                Log.e("NOTIFICATION", "Exception", e)
            }
        }
    }
}
package com.swathi.queue_app.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swathi.queue_app.model.CompleteCurrentResponse
import com.swathi.queue_app.model.MemberModel
import com.swathi.queue_app.model.MessageResponse
import com.swathi.queue_app.model.NextResponse
import com.swathi.queue_app.model.NotificationCountResponse
import com.swathi.queue_app.model.NotificationModel
import com.swathi.queue_app.model.NotificationReadResponse
import com.swathi.queue_app.model.QueueDetailsResponse
import com.swathi.queue_app.model.QueueStatusResponse

import com.swathi.queue_app.model.joinQueueResponse
import com.swathi.queue_app.model.myStatusResponse

import com.swathi.queue_app.repository.Queuerepository
import kotlinx.coroutines.launch



class Queueviewmodel : ViewModel() {
    private val repository = Queuerepository()
    val queueNotFound = MutableLiveData(false)
     val Joinqueueresponse= MutableLiveData<joinQueueResponse>()
    val createQueueResponse =
        MutableLiveData<MessageResponse>()
    val myStatusResponse = MutableLiveData<myStatusResponse>()
     fun joinQueue(
        queueId: String
    ) {

        viewModelScope.launch {

            try {

                val response =
                    repository.joinQueue(queueId)
                println(response.code())
                println(response.isSuccessful)
                println(response.errorBody()?.string())
                println(response.code())

                if(response.isSuccessful){

                    response.body()?.let {

                        Joinqueueresponse.value = it
                    }
                }

            } catch (e: Exception){

                println(e.message)
            }
        }
    }

    fun myStatus(queueId: String?) {

        viewModelScope.launch {

            try {

                println("CALLING API WITH = $queueId")

                val response =
                    repository.myStatus(queueId)

                println("SUCCESS = ${response.isSuccessful}")
                println("CODE = ${response.code()}")
                println("BODY = ${response.body()}")
                println("ERROR = ${response.errorBody()?.string()}")

                if (response.isSuccessful) {

                    response.body()?.let {

                        println("SETTING LIVE DATA = $it")

                        myStatusResponse.value = it
                    }
                }
                else if (response.code() == 404) {
                    queueNotFound.value = true
                }
            } catch (e: Exception) {

                println("EXCEPTION = ${e.message}")
            }
        }
    }
    val exitQueueResponse =
        MutableLiveData<String>()

    fun exitQueue(queueId: String) {

        viewModelScope.launch {

            try {

                val response =
                    repository.exitQueue(queueId)
println("Exit Queue response: ${response.code()}");
                println("${response.message()}")


                println("Code = ${response.code()}")
                println("Error = ${response.errorBody()?.string()}")
                if (response.code() == 404) {
                    queueNotFound.value = true
                }
                if(response.isSuccessful){

                    exitQueueResponse.value =
                        response.body()?.message
                }

            } catch (e: Exception){

                println(e.message)
            }
        }
    }
    val completeCurrentResponse =
        MutableLiveData<CompleteCurrentResponse>()

    fun completeCurrent(
        queueId: String
    ) {

        viewModelScope.launch {

            val response =
                repository.completeCurrent(queueId)

            if (response.isSuccessful) {

                response.body()?.let {

                    completeCurrentResponse.value = it
                }
            }
        }
    }
    fun resetQueueNotFound() {
        queueNotFound.value = false
    }
    val nextTokenResponse =
        MutableLiveData<NextResponse>()

    fun nextToken(
        queueId: String
    ) {

        viewModelScope.launch {

            val response =
                repository.nextToken(queueId)

            if(response.isSuccessful){

                response.body()?.let {

                    nextTokenResponse.value = it
                }
            }
        }
    }
    val queueStatusResponse =
        MutableLiveData<QueueStatusResponse>()

    fun toggleQueueStatus(
        queueId: String
    ) {

        viewModelScope.launch {

            val response =
                repository.toggleQueueStatus(queueId)

            if(response.isSuccessful){

                response.body()?.let {

                    queueStatusResponse.value = it
                }
            }
        }
    }
    val closeQueueResponse =
        MutableLiveData<MessageResponse>()

    fun closeQueue(
        queueId: String
    ) {

        viewModelScope.launch {

            val response =
                repository.closeQueue(queueId)

            if(response.isSuccessful){

                response.body()?.let {

                    closeQueueResponse.value = it
                }
            }
        }
    }
    val allMembersResponse =
        MutableLiveData<List<MemberModel>>()

    fun getAllMembers(
        queueId: String
    ) {

        viewModelScope.launch {

            val response =
                repository.getAllMembers(queueId)

            if(response.isSuccessful){

                response.body()?.let {

                    allMembersResponse.value = it
                }
            }
        }
    }
    val queueDetailsResponse =
        MutableLiveData<QueueDetailsResponse>()

    fun getQueueDetails(
        queueId: String
    ) {

        viewModelScope.launch {

            try {

                val response =
                    repository.getQueueDetails(
                        queueId
                    )
                println("Queue Details Response = ${response.body()}")
                println("Code = ${response.code()}")
                if(response.isSuccessful){

                    response.body()?.let {

                        queueDetailsResponse.value = it
                    }
                }

            } catch (e: Exception){

                println(e.message)
            }
        }
    }
    private val _userMembers = MutableLiveData<List<MemberModel>>()
    val userMembers: LiveData<List<MemberModel>> = _userMembers

    fun getUserMembers(queueId: String?) {
        viewModelScope.launch {
            Log.d("USER", "Calling API")

            val response = repository.getUserMembers(queueId)
            Log.d("USER", response.errorBody()?.string().toString())
            Log.d("USER", "Code = ${response.code()}")
            Log.d("USER", "Body = ${response.body()}")
            if (response.isSuccessful) {
                _userMembers.value = response.body()
            }
        }
    }



    fun createQueue(
        queueName: String,
        queueCapacity: Int,
        queueStatus: String
    ) {

        viewModelScope.launch {

            try {

                val response =
                    repository.createQueue(
                        queueName,
                        queueCapacity,
                        queueStatus
                    )

                if(response.isSuccessful){

                    response.body()?.let {

                        createQueueResponse.value = it
                    }

                } else {

                    println(
                        response.errorBody()?.string()
                    )
                }

            } catch (e: Exception){

                println(e.message)
            }
        }
    }


}
package com.swathi.queue_app.viewmodel.admin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.swathi.queue_app.model.adminDashboardresponse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swathi.queue_app.model.ActiveQueueResponse
import com.swathi.queue_app.model.MessageResponse
import com.swathi.queue_app.model.adminactivequeues

import com.swathi.queue_app.model.joinQueueResponse
import com.swathi.queue_app.model.myStatusResponse

import com.swathi.queue_app.repository.Queuerepository
import com.swathi.queue_app.repository.admin.Dashboardrepository
import kotlinx.coroutines.launch

class dashboardViewModel: ViewModel() {
    val dashboardResponse =
        MutableLiveData<adminDashboardresponse>()
val repository= Dashboardrepository()

    fun dashboard() {

        viewModelScope.launch {

            val response =
                repository.dashboard()

            if(response.isSuccessful){

                response.body()?.let {

                    dashboardResponse.value = it
                }
            }
        }
    }
    private val _activeQueues = MutableLiveData<List<adminactivequeues>>()
    val activeQueues: LiveData<List<adminactivequeues>> = _activeQueues
    fun getActiveQueues() {

        viewModelScope.launch {

            val response = repository.getActiveQueues()

            if (response.isSuccessful) {
                _activeQueues.value = response.body()
            }
        }
    }
}
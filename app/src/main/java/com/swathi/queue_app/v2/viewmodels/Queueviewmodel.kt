package com.swathi.queue_app.v2.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swathi.queue_app.v2.models.CreateQueueRequest
import com.swathi.queue_app.v2.models.VerifyHospitalResponse
import com.swathi.queue_app.v2.repo.AuthRepository
import com.swathi.queue_app.v2.repo.QueueRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class Queueviewmodel : ViewModel() {
    private val authRepository = AuthRepository()
    private val queueRepository = QueueRepository()


    private val _queueState = MutableStateFlow<Resource<Any>?>(null)
    val queueState: StateFlow<Resource<Any>?> get() = _queueState

    fun createQueue(hospitalId: String, department: String, doctorCode: String) {
        viewModelScope.launch {
            _queueState.value = Resource.Loading
            try {
                val request = CreateQueueRequest(hospitalId, department, doctorCode)
                val response = queueRepository.createQueue(request)

                Log.d("QueueViewModel", "${response.code()}")
                Log.d("QueueViewModel", "${response.body()}")

                if (response.isSuccessful && response.body()?.success == true) {
                    _queueState.value = Resource.Success(response.body()?.message ?: "Queue created successfully")
                } else {
                    _queueState.value = Resource.Error(response.errorBody()?.string() ?: "Failed to create queue")
                }
            } catch (e: Exception) {
                _queueState.value = Resource.Error(e.localizedMessage ?: "Network error occurred")
            }
        }
    }
}

sealed class Resource<out T> {
    object Loading : Resource<Nothing>()
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val message: String) : Resource<Nothing>()
}
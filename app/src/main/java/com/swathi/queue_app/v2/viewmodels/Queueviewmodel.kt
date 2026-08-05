package com.swathi.queue_app.v2.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swathi.queue_app.v2.models.CreateQueueRequest
import com.swathi.queue_app.v2.models.DashboardData
import com.swathi.queue_app.v2.models.QueueActionRequest
import com.swathi.queue_app.v2.models.SessionData
import com.swathi.queue_app.v2.repo.AuthRepository
import com.swathi.queue_app.v2.repo.QueueRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class Queueviewmodel : ViewModel() {
    private val authRepository = AuthRepository()
    private val queueRepository = QueueRepository()

    private val _dashboardState = MutableLiveData<DashboardState>()
    val dashboardState: LiveData<DashboardState> get() = _dashboardState

    // Change this line in your ViewModel:
    private val _queueState = MutableStateFlow<Resource<Any>?>(null)
    val queueState: StateFlow<Resource<Any>?> get() = _queueState
    fun createQueue(hospitalId: String, department: String, doctorCode: String) {
        viewModelScope.launch {
            _queueState.value = Resource.Loading
            try {
                val request = CreateQueueRequest(hospitalId, department, doctorCode)
                val response = queueRepository.createQueue(request)

                if (response.isSuccessful && response.body()?.success == true) {
                    _queueState.value = Resource.Success<Any>(response.body()?.message ?: "Queue created successfully")
                } else {
                    _queueState.value = Resource.Error(response.errorBody()?.string() ?: "Failed to create queue")
                }
            } catch (e: Exception) {
                _queueState.value = Resource.Error(e.localizedMessage ?: "Network error occurred")
            }
        }
    }

    fun fetchActiveSession(department: String, doctorCode: String) {
        viewModelScope.launch {
            _queueState.value = Resource.Loading
            try {
                val response = queueRepository.getActiveSession(department, doctorCode)

                if (response.isSuccessful && response.body()?.success == true) {
                    _queueState.value = Resource.Success<Any>(response.body()?.data as Any)
                } else {
                    _queueState.value = Resource.Error(response.errorBody()?.string() ?: "Failed to fetch active session")
                }
            } catch (e: Exception) {
                _queueState.value = Resource.Error(e.localizedMessage ?: "Network error occurred")
            }
        }
    }
    private val _navigationEvent = MutableLiveData<DoctorNavigationEvent>()
    val navigationEvent: LiveData<DoctorNavigationEvent> get() = _navigationEvent

    fun checkDoctorSession(department: String, doctorCode: String) {
        viewModelScope.launch {
            try {
                val response = queueRepository.getActiveSession(department, doctorCode)

                if (response.isSuccessful && response.body()?.success == true) {
                    // Active queue exists -> Trigger navigation to Home
                    _navigationEvent.value = DoctorNavigationEvent.NavigateToHome(department, doctorCode)
                } else {
                    // No active queue -> Trigger navigation to Department selection
                    _navigationEvent.value = DoctorNavigationEvent.NavigateToDepartmentSelection
                }
            } catch (e: Exception) {
                // Fallback to department selection on error so they aren't blocked
                _navigationEvent.value = DoctorNavigationEvent.NavigateToDepartmentSelection
            }
        }
    }
    fun loadDashboardData(userId: String) {
        _dashboardState.value = DashboardState.Loading
        viewModelScope.launch {
            // Using queueRepository to fetch the dashboard data
            val result = queueRepository.fetchDashboard(userId)
            if (result.isSuccess) {
                _dashboardState.value = DashboardState.Success(result.getOrNull())
            } else {
                _dashboardState.value = DashboardState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }
    }
    fun callNextPatient(department: String, doctorCode: String) {
        viewModelScope.launch {
            _queueState.value = Resource.Loading
            try {
                val request = QueueActionRequest(department, doctorCode)
                val response = queueRepository.callNextPatient(department,doctorCode)

                if (response.isSuccessful && response.body()?.success == true) {

                    _queueState.value = Resource.Success<Any>(response.body()?.data as Any)
                } else {
                    _queueState.value = Resource.Error(response.errorBody()?.string() ?: "Failed to call next patient")
                }
            } catch (e: Exception) {
                _queueState.value = Resource.Error(e.localizedMessage ?: "Network error occurred")
            }
        }
    }

    fun completeConsultation(department: String, doctorCode: String) {
        viewModelScope.launch {
            _queueState.value = Resource.Loading
            try {
                val request = QueueActionRequest(department, doctorCode)
                val response = queueRepository.completeCurrent(department,doctorCode)

                if (response.isSuccessful && response.body()?.success == true) {
                    _queueState.value = Resource.Success<Any>(response.body()?.data as Any)
                } else {
                    _queueState.value = Resource.Error(response.errorBody()?.string() ?: "Failed to complete consultation")
                }
            } catch (e: Exception) {
                _queueState.value = Resource.Error(e.localizedMessage ?: "Network error occurred")
            }
        }
    }
}

sealed class DashboardState {
    object Loading : DashboardState()
    data class Success(val data: DashboardData?) : DashboardState()
    data class Error(val message: String) : DashboardState()
}

sealed class Resource<out T> {
    object Loading : Resource<Nothing>()
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val message: String) : Resource<Nothing>()
}
sealed class DoctorNavigationEvent {
    data class NavigateToHome(val department: String, val doctorCode: String) : DoctorNavigationEvent()
    object NavigateToDepartmentSelection : DoctorNavigationEvent()
}
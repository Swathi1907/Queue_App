package com.swathi.queue_app.v2.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swathi.queue_app.v2.models.CreateQueueRequest
import com.swathi.queue_app.v2.models.DashboardData
import com.swathi.queue_app.v2.models.QueueActionRequest
import com.swathi.queue_app.v2.models.QueueTicketData
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

    private val _queueState = MutableStateFlow<Resource<Any>?>(null)
    val queueState: StateFlow<Resource<Any>?> get() = _queueState
private val _leaveQueueState=MutableStateFlow<Resource<Any>?>(null)
    val leaveQueueState:StateFlow<Resource<Any>?> get()=_leaveQueueState
    fun createQueue(hospitalId: String, department: String, doctorCode: String) {
        viewModelScope.launch {
            _queueState.value = Resource.Loading
            try {
                val request = CreateQueueRequest(hospitalId, department, doctorCode)
                val response = queueRepository.createQueue(request)

                if (response.isSuccessful && response.body()?.success == true) {
                    val body = response.body()
                    if (body?.data != null) {
                        _queueState.value = Resource.Success(body.data)
                    } else {
                        _queueState.value = Resource.Success(body?.message ?: "Queue created successfully")
                    }
                } else {
                    _queueState.value = Resource.Error(response.errorBody()?.string() ?: "Failed to create queue")
                }
            } catch (e: Exception) {
                _queueState.value = Resource.Error(e.localizedMessage ?: "Network error occurred")
            }
        }
    }

    fun updateQueueStatus(department: String, doctorCode: String, status: String) {
        viewModelScope.launch {
            _queueState.value = Resource.Loading
            try {
                val response = queueRepository.updateQueueStatus(department, doctorCode, status)

                if (response.isSuccessful && response.body()?.success == true) {
                    val body = response.body()
                    if (body?.data != null) {
                        _queueState.value = Resource.Success(body.data)
                    } else {
                        _queueState.value = Resource.Success(body?.message ?: "Status updated successfully")
                    }
                } else {
                    _queueState.value = Resource.Error(response.errorBody()?.string() ?: "Failed to update queue status")
                }
            } catch (e: Exception) {
                _queueState.value = Resource.Error(e.localizedMessage ?: "Network error occurred")
            }
        }
    }
    // Backing LiveData / StateFlow for the live ticket screen

    private val _liveTicketState = MutableStateFlow<Resource<QueueTicketData>?>(null)
    val liveTicketState: StateFlow<Resource<QueueTicketData>?> get() = _liveTicketState

    fun fetchLiveTicket(queueId: String,userId:String) {
        viewModelScope.launch {
            _liveTicketState.value = Resource.Loading

            try {
                val result = queueRepository.getLiveTicket(queueId,userId)

                if (result.isSuccess) {

                    val response = result.getOrNull()

                    if (response?.data != null) {
                        _liveTicketState.value =
                            Resource.Success(response.data)
                    } else {
                        _liveTicketState.value =
                            Resource.Error("Ticket data not found")
                    }

                } else {

                    _liveTicketState.value =
                        Resource.Error(
                            result.exceptionOrNull()?.message
                                ?: "Failed to fetch live ticket"
                        )
                }

            } catch (e: Exception) {

                _liveTicketState.value =
                    Resource.Error(
                        e.localizedMessage ?: "Network error occurred"
                    )
            }
        }
    }
    fun fetchActiveSession(department: String, doctorCode: String) {
        viewModelScope.launch {
            _queueState.value = Resource.Loading
            try {
                val response = queueRepository.getActiveSession(department, doctorCode)

                if (response.isSuccessful && response.body()?.success == true) {
                    val body = response.body()
                    if (body?.data != null) {
                        _queueState.value = Resource.Success(body.data)
                    } else {
                        // Safe fallback when data is null but success is true (e.g. no active session)
                        _queueState.value = Resource.Success(body?.message ?: "No active session found")
                    }
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

                if (response.isSuccessful && response.body()?.success == true && response.body()?.data != null) {
                    _navigationEvent.value = DoctorNavigationEvent.NavigateToHome(department, doctorCode)
                } else {
                    _navigationEvent.value = DoctorNavigationEvent.NavigateToDepartmentSelection
                }
            } catch (e: Exception) {
                _navigationEvent.value = DoctorNavigationEvent.NavigateToDepartmentSelection
            }
        }
    }

    fun loadDashboardData(userId: String) {
        _dashboardState.value = DashboardState.Loading
        viewModelScope.launch {
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
                val response = queueRepository.callNextPatient(department, doctorCode)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true && body.data != null) {
                        _queueState.value = Resource.Success(body.data)
                    } else {
                        _queueState.value = Resource.Error(body?.message ?: "Failed to call next patient")
                    }
                } else {
                    _queueState.value = Resource.Error("Failed to call next patient")
                }
            } catch (e: Exception) {
                _queueState.value = Resource.Error(e.localizedMessage ?: "Network error occurred")
            }
        }
    }


    fun leaveQueue(queueId: String) {
        viewModelScope.launch {
            _leaveQueueState.value = Resource.Loading
            try {
                val response = queueRepository.leaveQueue(queueId)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true) {
                        // Pass data or message depending on your Resource type
                        _leaveQueueState.value = Resource.Success(body.message ?: "Successfully left the queue")
                    } else {
                        _leaveQueueState.value = Resource.Error(body?.message ?: "Failed to leave queue")
                    }
                } else {
                    _leaveQueueState.value = Resource.Error("Failed to leave queue")
                }
            } catch (e: Exception) {
                _leaveQueueState.value = Resource.Error(e.localizedMessage ?: "Network error occurred")
            }
        }
    }
    fun completeConsultation(department: String, doctorCode: String) {
        viewModelScope.launch {
            _queueState.value = Resource.Loading
            try {
                val response = queueRepository.completeCurrent(department, doctorCode)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true && body.data != null) {
                        _queueState.value = Resource.Success(body.data)
                    } else {
                        _queueState.value = Resource.Error(body?.message ?: "Failed to complete consultation")
                    }
                } else {
                    _queueState.value = Resource.Error("Failed to complete consultation")
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
    data class NavigateToHome(val department: String, val doctorCode:String) : DoctorNavigationEvent()
    object NavigateToDepartmentSelection : DoctorNavigationEvent()
}
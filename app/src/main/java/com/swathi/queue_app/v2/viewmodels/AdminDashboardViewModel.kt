package com.swathi.queue_app.v2.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.swathi.queue_app.v2.models.AdminDashboardData
import com.swathi.queue_app.v2.models.DoctorDirectoryItem
import com.swathi.queue_app.v2.models.GlobalQueueItemRemote
import com.swathi.queue_app.v2.repo.AdminRepository
import kotlinx.coroutines.launch

class AdminDashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AdminRepository(application)
    private val _globalQueues = MutableLiveData<List<GlobalQueueItemRemote>>()
    val globalQueues: LiveData<List<GlobalQueueItemRemote>> get() = _globalQueues
    // Dashboard Stats LiveData
    private val _dashboardStats = MutableLiveData<AdminDashboardData?>()
    val dashboardStats: LiveData<AdminDashboardData?> get() = _dashboardStats

    // Doctor Directory LiveData
    private val _doctorDirectory = MutableLiveData<List<DoctorDirectoryItem>>()
    val doctorDirectory: LiveData<List<DoctorDirectoryItem>> get() = _doctorDirectory

    // Common Loading & Error LiveData
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    fun fetchDashboardStats() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val result = repository.getDashboardStats()
            _isLoading.value = false

            result.onSuccess { data ->
                _dashboardStats.value = data
            }.onFailure { exception ->
                _errorMessage.value = exception.localizedMessage ?: "Unknown error occurred"
            }
        }
    }

    fun fetchDoctorDirectory(hospitalId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val result = repository.getDoctorDirectory(hospitalId)
            _isLoading.value = false

            result.onSuccess { response ->
                if (response.success) {
                    _doctorDirectory.value = response.data ?: emptyList()
                } else {
                    _errorMessage.value = response.message
                }
            }.onFailure { exception ->
                _errorMessage.value = exception.localizedMessage ?: "Failed to load doctor directory"
            }
        }
    }
    fun fetchGlobalQueues() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val result = repository.getGlobalDepartments()
            _isLoading.value = false

            result.onSuccess { wrapper ->
                if (wrapper.success) {
                    _globalQueues.value = wrapper.data ?: emptyList()
                } else {
                    _errorMessage.value = "Failed to load global queues"
                }
            }.onFailure { exception ->
                _errorMessage.value = exception.localizedMessage ?: "Unknown error occurred"
            }
        }
    }
}
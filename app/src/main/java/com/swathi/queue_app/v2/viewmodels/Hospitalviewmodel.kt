package com.swathi.queue_app.v2.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swathi.queue_app.v2.models.DepartmentData
import com.swathi.queue_app.v2.models.Doctor
import com.swathi.queue_app.v2.models.Hospital
import com.swathi.queue_app.v2.repo.HospitalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HospitalViewModel : ViewModel() {

    private val repository = HospitalRepository()

    private val _doctorState = MutableStateFlow<Resource<List<Doctor>>>(Resource.Idle)
    val doctorState: StateFlow<Resource<List<Doctor>>> = _doctorState.asStateFlow()

    private val _departmentState = MutableStateFlow<Resource<DepartmentData>?>(null)
    val departmentState: StateFlow<Resource<DepartmentData>?> = _departmentState

    fun fetchDepartments(hospitalId: String) {
        viewModelScope.launch {
            _departmentState.value = Resource.Loading
            try {
                Log.d("hospview","called get")
                val response = repository.getDepartments(hospitalId)
                val body = response?.body()
                Log.d("hospview","${response.code()}")
                if (response != null && response.isSuccessful && body?.success == true && body.data != null) {
                    // body.data is now safely non-null (DepartmentData)
                    _departmentState.value = Resource.Success(body.data)
                } else {
                    _departmentState.value = Resource.Error("Failed to load departments")
                }
            } catch (e: Exception) {
                _departmentState.value = Resource.Error(e.message ?: "Unknown error occurred")
            }
        }
    }


    fun fetchDoctors(hospitalId: String, department: String) {
        viewModelScope.launch {
            _doctorState.value = Resource.Loading
            try {
                val response = repository.getDoctors(hospitalId, department)
                val body = response?.body()
                if (response != null && response.isSuccessful && body?.success == true) {
                    _doctorState.value = Resource.Success(body.data)
                } else {
                    _doctorState.value = Resource.Error(body?.message ?: "Failed to load doctors")
                }
            } catch (e: Exception) {
                _doctorState.value = Resource.Error(e.message ?: "Unknown error occurred")
            }
        }

    }
    private val _hospitals = MutableLiveData<List<Hospital>>()
    val hospitals: LiveData<List<Hospital>> get() = _hospitals

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    fun loadHospitals() {
        viewModelScope.launch {
            val result = repository.fetchHospitals()
            result.onSuccess { list ->
                _hospitals.value = list
            }.onFailure { exception ->
                _error.value = exception.localizedMessage ?: "Unknown error occurred"
            }
        }
    }
    sealed class Resource<out T> {
        object Idle : Resource<Nothing>()
        object Loading : Resource<Nothing>()
        data class Success<T>(val data: T) : Resource<T>()
        data class Error(val message: String) : Resource<Nothing>()
    }
}
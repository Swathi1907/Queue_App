package com.swathi.queue_app.v2.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swathi.queue_app.v2.models.DoctorProfileResponse
import com.swathi.queue_app.v2.models.ResumeScanResponse
import com.swathi.queue_app.v2.repo.DoctorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MultipartBody

class DoctorViewModel(
    private val repository: DoctorRepository = DoctorRepository()
) : ViewModel() {

    private val _doctorProfileState = MutableStateFlow<Resource<DoctorProfileResponse>>(Resource.Idle)
    val doctorProfileState: StateFlow<Resource<DoctorProfileResponse>> = _doctorProfileState.asStateFlow()

    fun fetchDoctorProfile() {
        viewModelScope.launch {
            _doctorProfileState.value = Resource.Loading
            val result = repository.getDoctorProfile()

            result.onSuccess { response ->
                _doctorProfileState.value = Resource.Success(response)
            }.onFailure { exception ->
                _doctorProfileState.value = Resource.Error(exception.localizedMessage ?: "Unknown error occurred")
            }
        }
    }
    private val _resumeScanState = MutableStateFlow<Resource<ResumeScanResponse>>(Resource.Idle)
    val resumeScanState: StateFlow<Resource<ResumeScanResponse>> = _resumeScanState.asStateFlow()
    fun scanDoctorResume(hospitalId: String, resumePart: MultipartBody.Part) {
        viewModelScope.launch {
            _resumeScanState.value = Resource.Loading
            val result = repository.scanDoctorResume(hospitalId, resumePart)

            result.onSuccess { response ->
                // Note: response.success can be false if no matching department is found,
                // but the API returned a 200 OK containing availableDepartments.
                _resumeScanState.value = Resource.Success(response)
            }.onFailure { exception ->
                _resumeScanState.value = Resource.Error(exception.localizedMessage ?: "Failed to scan resume")
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
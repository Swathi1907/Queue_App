package com.swathi.queue_app.v2.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swathi.queue_app.v2.models.DoctorProfileResponse
import com.swathi.queue_app.v2.repo.DoctorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

    sealed class Resource<out T> {
        object Idle : Resource<Nothing>()
        object Loading : Resource<Nothing>()
        data class Success<T>(val data: T) : Resource<T>()
        data class Error(val message: String) : Resource<Nothing>()
    }
}
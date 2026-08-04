package com.swathi.queue_app.v2.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swathi.queue_app.v2.models.AuthResponse
import com.swathi.queue_app.v2.models.LoginRequest
import com.swathi.queue_app.v2.models.SignupRequest
import com.swathi.queue_app.v2.models.VerifyDoctorCodeRequest
import com.swathi.queue_app.v2.models.VerifyDoctorCodeResponse
import com.swathi.queue_app.v2.models.VerifyHospitalRequest
import com.swathi.queue_app.v2.models.VerifyHospitalResponse
import com.swathi.queue_app.v2.repo.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val authRepository = AuthRepository()

    private val _loginState = MutableStateFlow<Resource<AuthResponse>?>(null)
    val loginState: StateFlow<Resource<AuthResponse>?> get() = _loginState

    // In AuthViewModel.kt
    private val _signupState = MutableStateFlow<Resource<AuthResponse>?>(null)
    val signupState: StateFlow<Resource<AuthResponse>?> get() = _signupState

    private val _verificationState = MutableStateFlow<Resource<VerifyHospitalResponse>?>(null)
    val verificationState: StateFlow<Resource<VerifyHospitalResponse>?> get() = _verificationState

    private val _doctorCodeVerificationState =
        MutableStateFlow<Resource<VerifyDoctorCodeResponse>?>(null)
    val doctorCodeVerificationState: StateFlow<Resource<VerifyDoctorCodeResponse>?> get() = _doctorCodeVerificationState

    fun login(request: LoginRequest) {
        viewModelScope.launch {
            _loginState.value = Resource.Loading
            try {
                val response = authRepository.login(request)
                Log.d("AuthViewModel", "${response.code()}")
                Log.d("AuthViewModel", "${response.body()}")
Log.d("Authviewmodel","stopping")
                if (response.isSuccessful) {
                    Log.d("authviemodel", "success")
                    val body = response.body()
                    if (body != null) {
                        _loginState.value = Resource.Success(body)
                    } else {
                        _loginState.value = Resource.Error("Login response body is null")
                    }
                } else {
                    _loginState.value =
                        Resource.Error(response.errorBody()?.string() ?: "Login failed")
                }
            } catch (e: Exception) {
                _loginState.value = Resource.Error(e.localizedMessage ?: "Network error occurred")
            }
        }
    }

    fun signup(request: SignupRequest) {
        viewModelScope.launch {
            _signupState.value = Resource.Loading
            try {
                val response = authRepository.signup(request)
                Log.d("signup","${response.code()}")
                Log.d("signup","${response.body()}")
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        // Explicitly pass the AuthResponse type
                        _signupState.value = Resource.Success(body)
                        Log.d("authview", "${_signupState.value}")
                    } else {
                        _signupState.value = Resource.Error("Response body is null")
                    }
                } else {
                    _signupState.value =
                        Resource.Error(response.errorBody()?.string() ?: "Signup failed")
                }
            } catch (e: Exception) {
                _signupState.value = Resource.Error(e.localizedMessage ?: "Network error occurred")
            }
        }

    }

    // Step 1: Verify Hospital / Initial Login
    fun verifyHospital(email: String, password: String, hospitalId: String) {
        viewModelScope.launch {
            _verificationState.value = Resource.Loading
            try {
                val request = VerifyHospitalRequest(email, password, hospitalId)
                val response = authRepository.verifyHospitalId(request)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    Log.d("authview", "Success: $body")
                    _verificationState.value = Resource.Success(body)
                } else {
                    _verificationState.value = Resource.Error(
                        response.errorBody()?.string() ?: "Hospital verification failed"
                    )
                }
            } catch (e: Exception) {
                _verificationState.value = Resource.Error(e.localizedMessage ?: "Network error")
            }
        }
    }// Step 2: Verify Doctor Code (if role is DOCTOR)
    fun verifyDoctorCode(email: String, password: String, hospitalId: String, doctorCode: String) {
        viewModelScope.launch {
            _doctorCodeVerificationState.value = Resource.Loading
            try {
                val request = VerifyDoctorCodeRequest(email, password, hospitalId, doctorCode)
                val response = authRepository.verifyDoctor(request)
Log.d("authview","${response.body()}")
                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        _doctorCodeVerificationState.value = Resource.Success(body)
                    } ?: run {
                        // Fixed: update _doctorCodeVerificationState instead of _verificationState
                        _doctorCodeVerificationState.value = Resource.Error("Response body is null")
                    }
                } else {
                    // Added: handle unsuccessful response codes properly
                    _doctorCodeVerificationState.value = Resource.Error(
                        response.errorBody()?.string() ?: "Doctor verification failed"
                    )
                }
            } catch (e: Exception) {
                // Fixed: update _doctorCodeVerificationState instead of _verificationState
                _doctorCodeVerificationState.value = Resource.Error(e.localizedMessage ?: "Network error")
            }
        }
    }

    sealed class Resource<out T> {
        object Loading : Resource<Nothing>()
        data class Success<out T>(val data: T) : Resource<T>()
        data class Error(val message: String) : Resource<Nothing>()
    }
}
package com.swathi.queue_app.viewmodel
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swathi.queue_app.model.Loginresponse
import com.swathi.queue_app.model.ProfileResponse
import com.swathi.queue_app.model.errorResponse
import com.swathi.queue_app.model.signupresponse
import com.swathi.queue_app.repository.Authrepository
import kotlinx.coroutines.launch
class AuthViewModel : ViewModel(){
    private val repository= Authrepository()
    val loginresponse = MutableLiveData<Loginresponse>()
    val signupResponse = MutableLiveData<signupresponse>()
    val errorResponse = MutableLiveData<errorResponse>()
    fun login(
        email: String,
        password: String
    ) {
        viewModelScope.launch {

            try {

                println("View model login started")

                val response =
                    repository.login(email, password)

                println("Response received")
                println(response.code())
                println(response.body())
                if (response.isSuccessful) {

                    println("SUCCESS")

                    response.body()?.let {

                        println("BODY RECEIVED")
                        println(it)

                        loginresponse.value = it

                        println("LIVEDATA UPDATED")
                    }

                } else {

                    println("FAILED")
                    println(response.code())
                    println(response.errorBody()?.string())
                }

            } catch (e: Exception) {

                println("ERROR = ${e.message}")
                e.printStackTrace()
            }
        }
    }
        fun signup(
            name: String,
            email: String,
            password: String
        ) {

            viewModelScope.launch {

                try {

                    val response =
                        repository.signup(
                            name,
                            email,
                            password
                        )
                    Log.d("SIGNUP", response.code().toString())
                    Log.d("SIGNUP", response.errorBody()?.string() ?: "")
                    if(response.isSuccessful){

                        response.body()?.let {

                            signupResponse.value = it
                        }
                    }
else{


                            val errorMessage =
                                response.errorBody()?.string()

                            errorResponse.value =
                                errorResponse(
                                    errorMessage ?: "Signup failed"
                                )

                    }
                } catch (e: Exception){

                    println(e.message)
                }
            }
        }
    val profileResponse =
        MutableLiveData<ProfileResponse>()

    fun getProfile() {

        viewModelScope.launch {

            val response =
                repository.getProfile()

            if (response.isSuccessful) {

                response.body()?.let {

                    profileResponse.value = it
                }
            }
        }
    }
    }


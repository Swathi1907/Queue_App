package com.swathi.queue_app.v2.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.swathi.queue_app.v2.viewmodels.AuthViewModel.Resource
import androidx.lifecycle.Lifecycle
import com.swathi.queue_app.v2.Activities.DoctorMainActivity
import com.swathi.queue_app.v2.Activities.CompounderMainActivity
import com.swathi.queue_app.v2.Activities.HospitalAdminActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.swathi.queue_app.databinding.SignupBinding
import com.swathi.queue_app.v2.Activities.MainActivity
import com.swathi.queue_app.v2.models.AuthResponse
import com.swathi.queue_app.v2.models.SignupRequest
import com.swathi.queue_app.v2.models.UserData
import com.swathi.queue_app.v2.utilis.TokenManager
import com.swathi.queue_app.v2.viewmodels.AuthViewModel

import kotlinx.coroutines.launch

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: SignupBinding
    private val authViewModel: AuthViewModel by viewModels()
    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setSoftInputMode(
            android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )

        binding = SignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tokenManager = TokenManager(this)

        setupClickListeners()
        observeSignupState()
    }

    private fun setupClickListeners() {
        binding.btnSignup.setOnClickListener {

            Log.d("SIGNUP_DEBUG", "SIGN UP BUTTON CLICKED")

            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            Log.d(
                "SIGNUP_DEBUG",
                "name=$name, email=$email, phone=$phone"
            )

            if (name.isEmpty() || phone.isEmpty() || password.isEmpty()) {
                Log.d("SIGNUP_DEBUG", "VALIDATION FAILED")

                Toast.makeText(
                    this,
                    "Please fill out required fields",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            Log.d("SIGNUP_DEBUG", "VALIDATION PASSED")

            val request = SignupRequest(
                name = name,
                email = if (email.isEmpty()) null else email,
                phoneNumber = phone,
                password = password
            )

            Log.d("SIGNUP_DEBUG", "Calling authViewModel.signup()")

            authViewModel.signup(request)
        }
        binding.tvlogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }
    private fun observeSignupState() {

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                authViewModel.signupState.collect { resource ->

                    Log.d(
                        "SIGNUP_ACTIVITY",
                        "STATE RECEIVED = $resource"
                    )

                    if (resource == null) return@collect

                    when (resource) {

                        is Resource.Loading -> {

                            Log.d(
                                "SIGNUP_ACTIVITY",
                                "LOADING"
                            )

                            binding.btnSignup.isEnabled = false
                        }

                        is Resource.Error -> {

                            Log.e(
                                "SIGNUP_ACTIVITY",
                                "ERROR = ${resource.message}"
                            )

                            binding.btnSignup.isEnabled = true

                            Toast.makeText(
                                this@SignupActivity,
                                resource.message,
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        is Resource.Success<*> -> {

                            Log.d(
                                "SIGNUP_ACTIVITY",
                                "SUCCESS BLOCK REACHED"
                            )

                            binding.btnSignup.isEnabled = true

                            val authResponse =
                                resource.data as? AuthResponse

                            if (authResponse == null) {
                                Log.e(
                                    "SIGNUP_ACTIVITY",
                                    "AuthResponse CAST FAILED"
                                )
                                return@collect
                            }

                            Log.d(
                                "SIGNUP_ACTIVITY",
                                "AuthResponse = $authResponse"
                            )

                            val userData = authResponse.data

                            if (userData == null) {
                                Log.e(
                                    "SIGNUP_ACTIVITY",
                                    "USER DATA IS NULL"
                                )
                                return@collect
                            }

                            val userRole =
                                (userData.role ?: "PATIENT")
                                    .uppercase(java.util.Locale.ROOT)

                            Log.d(
                                "SIGNUP_ACTIVITY",
                                "ROLE = $userRole"
                            )

                            val token = userData.jwt_token

                            Log.d(
                                "SIGNUP_ACTIVITY",
                                "TOKEN EXISTS = ${!token.isNullOrEmpty()}"
                            )

                            if (token.isNullOrEmpty()) {

                                Toast.makeText(
                                    this@SignupActivity,
                                    "Signup successful but token is missing",
                                    Toast.LENGTH_LONG
                                ).show()

                                return@collect
                            }

                            // Save JWT + role
                            tokenManager.saveAuthData(
                                token,
                                userRole
                            )


                            tokenManager.saveUserProfile(
                                userData._id ?: "",
                                userData.name ?: ""
                            )

                            tokenManager.savecontact(
                                userData.phoneNumber ?: ""
                            )

                            tokenManager.saveCredentials(
                                userData.email ?: "",
                                ""
                            )

                            Log.d(
                                "SIGNUP_ACTIVITY",
                                "AUTH + NAME + EMAIL + PHONE SAVED"
                            )
                            Log.d(
                                "SIGNUP_ACTIVITY",
                                "AUTH DATA SAVED"
                            )

                            val intent = when (userRole) {

                                "PATIENT" -> {
                                    Log.d(
                                        "SIGNUP_ACTIVITY",
                                        "GOING TO MAIN ACTIVITY"
                                    )

                                    Intent(
                                        this@SignupActivity,
                                        MainActivity::class.java
                                    )
                                }

                                "DOCTOR" -> {
                                    Intent(
                                        this@SignupActivity,
                                        DoctorMainActivity::class.java
                                    ).apply {
                                        putExtra(
                                            "NAVIGATE_TO",
                                            "DEPARTMENTS"
                                        )
                                    }
                                }

                                "COMPOUNDER" -> {
                                    Intent(
                                        this@SignupActivity,
                                        CompounderMainActivity::class.java
                                    )
                                }

                                "ADMIN",
                                "SUPER_ADMIN" -> {
                                    Intent(
                                        this@SignupActivity,
                                        HospitalAdminActivity::class.java
                                    )
                                }

                                else -> {
                                    Log.e(
                                        "SIGNUP_ACTIVITY",
                                        "UNKNOWN ROLE = $userRole"
                                    )

                                    Toast.makeText(
                                        this@SignupActivity,
                                        "Unknown role: $userRole",
                                        Toast.LENGTH_LONG
                                    ).show()

                                    return@collect
                                }
                            }

                            Log.d(
                                "SIGNUP_ACTIVITY",
                                "STARTING ACTIVITY = ${intent.component}"
                            )

                            startActivity(intent)
                            finish()

                            Log.d(
                                "SIGNUP_ACTIVITY",
                                "SIGNUP ACTIVITY FINISHED"
                            )
                        }
                    }
                }
            }
        }
    }
  }
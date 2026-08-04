package com.swathi.queue_app.v2.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.swathi.queue_app.databinding.SignupBinding
import com.swathi.queue_app.v2.Activities.MainActivity
import com.swathi.queue_app.v2.models.AuthResponse
import com.swathi.queue_app.v2.models.SignupRequest
import com.swathi.queue_app.v2.models.UserData
import com.swathi.queue_app.v2.utilis.TokenManager
import com.swathi.queue_app.v2.viewmodels.AuthViewModel
import com.swathi.queue_app.v2.viewmodels.Resource
import kotlinx.coroutines.launch

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: SignupBinding
    private val authViewModel: AuthViewModel by viewModels()
    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tokenManager = TokenManager(this)

        setupClickListeners()
        observeSignupState()
    }

    private fun setupClickListeners() {
        binding.btnSignup.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (name.isEmpty() || phone.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill out required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Log.d("SignupActivity", "Your debug message here: $email")
            val request = SignupRequest(
                name = name,
                email = if (email.isEmpty()) null else email,
                phoneNumber = phone,
                password = password
            )

            authViewModel.signup(request)
        }
    }

    private fun observeSignupState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.signupState.collect { resource ->
                    if (resource == null) return@collect

                    when (resource) {
                        is Resource.Loading -> {
                            binding.btnSignup.isEnabled = false
                        }
                        is Resource.Error -> {
                            binding.btnSignup.isEnabled = true
                            Toast.makeText(this@SignupActivity, resource.message, Toast.LENGTH_LONG).show()
                        }
                        is Resource.Success<*> -> {
                            binding.btnSignup.isEnabled = true
                            val authResponse = resource.data as? AuthResponse ?: return@collect
                            val userData = authResponse.data
                            val userRole = (userData?.role ?: "PATIENT").uppercase(java.util.Locale.ROOT)

                            if (userData != null) {
                                val userId = userData._id
                                val token = userData.jwt_token ?: ""
                                val userEmail = userData.email ?: binding.etEmail.text.toString().trim()
                                val contact = userData.phoneNumber ?: binding.etPhone.text.toString().trim()

                                tokenManager.saveAuthData(token, userRole)
                                tokenManager.saveUserProfile(userId, userData.name)
                                tokenManager.saveCredentials(userEmail, "")
                                tokenManager.savecontact(contact)
                            }

                            Toast.makeText(this@SignupActivity, "Registration Successful!", Toast.LENGTH_SHORT).show()

                            val intent = Intent(
                                this@SignupActivity,
                                com.swathi.queue_app.v2.Activities.MainActivity::class.java
                            ).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            startActivity(intent)
                            finish()
                        }
                        else -> {} // Satisfies Kotlin's exhaustiveness requirement
                    }
                }
            }
        }
    }
}
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
import com.swathi.queue_app.databinding.LoginBinding
import com.swathi.queue_app.v2.Activities.DoctorMainActivity
import com.swathi.queue_app.v2.models.AuthResponse
import com.swathi.queue_app.v2.models.LoginRequest
import com.swathi.queue_app.v2.viewmodels.AuthViewModel
import com.swathi.queue_app.v2.viewmodels.AuthViewModel.Resource
import com.swathi.queue_app.v2.utilis.TokenManager
import kotlinx.coroutines.launch
import com.swathi.queue_app.v2.Activities.MainActivity
import com.swathi.queue_app.v2.Activities.CompounderMainActivity
import com.swathi.queue_app.v2.Activities.HospitalAdminActivity
 // Make sure this import matches your package structure

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: LoginBinding
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check for an existing active session before rendering the UI
        val tokenManager = TokenManager(this)
        val token = tokenManager.getToken()
        val role = tokenManager.getUserrole()?.uppercase(java.util.Locale.ROOT)

        if (!token.isNullOrEmpty() && !role.isNullOrEmpty()) {
            val targetIntent = when (role) {
                "ADMIN", "SUPER_ADMIN" -> Intent(this, HospitalAdminActivity::class.java)
                "DOCTOR" -> Intent(this, DoctorMainActivity::class.java).apply {
                    putExtra("NAVIGATE_TO", "DEPARTMENTS")
                }
                "COMPOUNDER" -> Intent(this, CompounderMainActivity::class.java)
                "PATIENT" -> Intent(this, MainActivity::class.java)
                else -> null
            }

            if (targetIntent != null) {
                startActivity(targetIntent)
                finish()
                return
            }
        }

        binding = LoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupClickListeners()
        observeLoginState()

        binding.tvsignup.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            Log.d("login", "clicked")
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Direct login call for all roles
            Log.d("login", "calling direct login")
            val request = LoginRequest(email = email, password = password, role = "")
            authViewModel.login(request)
        }
    }

    private fun observeLoginState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.loginState.collect { resource ->
                    if (resource == null) return@collect

                    when (resource) {
                        is Resource.Loading -> {
                            binding.btnLogin.isEnabled = false
                        }
                        is Resource.Error -> {
                            binding.btnLogin.isEnabled = true
                            Log.d("login", "${resource.message}")
                            Toast.makeText(this@LoginActivity, resource.message, Toast.LENGTH_LONG).show()
                        }
                        is Resource.Success<*> -> {
                            binding.btnLogin.isEnabled = true
                            val authResponse = resource.data as? AuthResponse ?: return@collect
                            val userData = authResponse.data
                            val userRole = (userData?.role ?: "").uppercase(java.util.Locale.ROOT)
                            val tokenManager = TokenManager(this@LoginActivity)

                            // Save credentials and general user profile data
                            val emailInput = binding.etEmail.text.toString().trim()
                            val passwordInput = binding.etPassword.text.toString().trim()
                            tokenManager.saveCredentials(emailInput, passwordInput)
                            tokenManager.saveUserProfile(userData?._id ?: "", userData?.name ?: "")

                            // Save role-specific properties if available in user object
                            userData?.hospitalId?.let { tokenManager.saveHospitalId(it) }
                            userData?.doctorCode?.let { tokenManager.saveDoctorCode(it) }

                            // Extract JWT token and navigate based strictly on role
                            userData?.jwt_token?.let { token ->
                                tokenManager.saveAuthData(token, userRole)
                                Toast.makeText(this@LoginActivity, "Login Successful!", Toast.LENGTH_SHORT).show()

                                val targetIntent = when (userRole) {
                                    "ADMIN", "SUPER_ADMIN" -> Intent(this@LoginActivity, HospitalAdminActivity::class.java)
                                    "DOCTOR" -> Intent(this@LoginActivity, DoctorMainActivity::class.java).apply {
                                        putExtra("NAVIGATE_TO", "DEPARTMENTS")
                                    }
                                    "COMPOUNDER" -> Intent(this@LoginActivity, CompounderMainActivity::class.java)
                                    "PATIENT" -> Intent(this@LoginActivity, MainActivity::class.java)
                                    else -> {
                                        Toast.makeText(this@LoginActivity, "Unknown user role: $userRole", Toast.LENGTH_LONG).show()
                                        null
                                    }
                                }

                                targetIntent?.let {
                                    startActivity(it)
                                    finish()
                                }
                            } ?: run {
                                Toast.makeText(this@LoginActivity, "Authentication token missing", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
        }
    }
}
package com.swathi.queue_app.v2.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.swathi.queue_app.databinding.LoginBinding
import com.swathi.queue_app.databinding.UserMainActivityBinding
import com.swathi.queue_app.v2.Activities.DoctorMainActivity
import com.swathi.queue_app.v2.models.AuthResponse
import com.swathi.queue_app.v2.models.LoginRequest
import com.swathi.queue_app.v2.models.VerifyDoctorCodeResponse
import com.swathi.queue_app.v2.models.VerifyHospitalResponse
import com.swathi.queue_app.v2.viewmodels.AuthViewModel
import com.swathi.queue_app.v2.viewmodels.AuthViewModel.Resource
import com.swathi.queue_app.v2.utilis.TokenManager
import kotlinx.coroutines.launch
import com.swathi.queue_app.v2.Activities.MainActivity
import com.swathi.queue_app.v2.Activities.CompounderMainActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: LoginBinding
    private val authViewModel: AuthViewModel by viewModels()
    // Add this near the top with your other viewModels:
    private val queueViewModel: com.swathi.queue_app.v2.viewmodels.Queueviewmodel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupClickListeners()
        observeLoginStates()
        binding.tvSubTitle.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
        }
    }
    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Check if we are currently waiting for a Doctor Code submission
            if (binding.tilDoctorCode.visibility == View.VISIBLE) {
                val hospitalId = binding.etHospitalId.text.toString().trim()
                val doctorCode = binding.etDoctorCode.text.toString().trim()
                if (hospitalId.isEmpty() || doctorCode.isEmpty()) {
                    Toast.makeText(this, "Hospital ID and Doctor Code are required", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                Log.d("login","verifydoctor called")

                authViewModel.verifyDoctorCode(email, password, hospitalId, doctorCode)
            }
            // Check if we are currently waiting for a Hospital ID submission
            else if (binding.hospitalIdLayout.visibility == View.VISIBLE) {
                val hospitalId = binding.etHospitalId.text.toString().trim()
                if (hospitalId.isEmpty()) {
                    Toast.makeText(this, "Hospital ID is required", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                authViewModel.verifyHospital(email, password, hospitalId)
            }
            // Default entry point: Always fire standard login first so backend determines the role
            else {
                Log.d("login", "calling initial login")
                val request = LoginRequest(email = email, password = password, role = "PATIENT")
                authViewModel.login(request)
            }
        }
    }

    private fun observeLoginStates() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                // 1. Observe Initial Login State (Driven by backend role response)
                launch {
                    authViewModel.loginState.collect { resource ->
                        if (resource == null) return@collect

                        when (resource) {
                            is Resource.Loading -> {
                                binding.btnLogin.isEnabled = false
                            }
                            is Resource.Error -> {
                                binding.btnLogin.isEnabled = true
                                Log.d("login","${resource.message}")
                                Toast.makeText(this@LoginActivity, resource.message, Toast.LENGTH_LONG).show()
                            }
                            // Inside your observeLoginStates() -> loginState Success block:
                            // Inside your observeLoginStates() -> loginState Success block:
                            is Resource.Success<*> -> {
                                binding.btnLogin.isEnabled = true
                                val authResponse = resource.data as? AuthResponse ?: return@collect
                                val userData = authResponse.data
                                val userRole = (userData?.role ?: "ntg").uppercase(java.util.Locale.ROOT)
                                val tokenManager = TokenManager(this@LoginActivity)

                                // Securely save credentials for multi-step verification flows
                                val emailInput = binding.etEmail.text.toString().trim()
                                val passwordInput = binding.etPassword.text.toString().trim()
                                tokenManager.saveCredentials(emailInput, passwordInput)
tokenManager.saveUserProfile(userData?._id ?: "", userData?.name ?: "")
                                Log.d("login","${userData?._id} is null")
                                if (userRole == "DOCTOR" || userRole == "COMPOUNDER") {
                                    Toast.makeText(this@LoginActivity, "Please enter your Hospital ID.", Toast.LENGTH_SHORT).show()
                                    binding.hospitalIdLayout.visibility = View.VISIBLE
                                    binding.btnLogin.text = "Verify Hospital"
                                } else {
                                    userData?.jwt_token?.let { token ->
                                        tokenManager.saveAuthData(token, userRole)
                                        Toast.makeText(this@LoginActivity, "Login Successful!", Toast.LENGTH_SHORT).show()

                                        val intent = Intent(this@LoginActivity,
                                           MainActivity::class.java)
                                        startActivity(intent)

                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Observe Hospital Verification State (Handles Compounder completion or triggers Doctor Code prompt)
                launch {
                    authViewModel.verificationState.collect { resource ->
                        if (resource == null) return@collect

                        when (resource) {
                            is Resource.Loading -> {
                                binding.btnLogin.isEnabled = false
                            }
                            is Resource.Error -> {
                                binding.btnLogin.isEnabled = true
                                Log.d("login","${resource.message}")
                                Toast.makeText(this@LoginActivity, resource.message, Toast.LENGTH_LONG).show()
                            }
                            is Resource.Success<*> -> {
                                binding.btnLogin.isEnabled = true
                                val verifyResponse = resource.data as? VerifyHospitalResponse ?: return@collect
                                val responseData = verifyResponse.data
                                val userRole = (responseData?.role ?: "ntg").uppercase(java.util.Locale.ROOT)
                                val hospitalId = binding.etHospitalId.text.toString().trim()
                                val tokenManager = TokenManager(this@LoginActivity)

                                if (verifyResponse.requiresDoctorCode == true) {
                                    // Doctor workflow: Dynamically reveal Doctor Code field
                                    if (binding.tilDoctorCode.visibility != View.VISIBLE) {
                                        Log.d("login","${verifyResponse.message}")
                                        Toast.makeText(this@LoginActivity, verifyResponse.message, Toast.LENGTH_SHORT).show()
                                        tokenManager.saveHospitalId(hospitalId)
                                        binding.tilDoctorCode.visibility = View.VISIBLE
                                        binding.btnLogin.text = "Complete Doctor Login"
                                    }
                                } else {
                                    // Compounder workflow: Finishes login immediately upon hospital verification
                                    tokenManager.saveHospitalId(hospitalId)
                                    responseData?.jwt_token?.let { token ->
                                        tokenManager.saveAuthData(token, userRole)
                                        Toast.makeText(this@LoginActivity, verifyResponse.message, Toast.LENGTH_SHORT).show()
                                        val intent = Intent(this@LoginActivity, CompounderMainActivity::class.java)
                                        startActivity(intent)

                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Observe Doctor Code Verification State (Final Step for Doctors)
                // 3. Observe Doctor Code Verification State (Final Step for Doctors)
                launch {
                    authViewModel.doctorCodeVerificationState.collect { resource ->
                        if (resource == null) return@collect

                        when (resource) {
                            is Resource.Loading -> {
                                binding.btnLogin.isEnabled = false
                            }
                            is Resource.Error -> {
                                binding.btnLogin.isEnabled = true
                                Log.d("login doctor","${resource.message}")
                                Toast.makeText(this@LoginActivity, resource.message, Toast.LENGTH_LONG).show()
                            }
                            is Resource.Success<*> -> {
                                binding.btnLogin.isEnabled = true
                                val responseBody = resource.data as? VerifyDoctorCodeResponse
                                val token = responseBody?.data?.jwt_token
                                val doctorCode = responseBody?.data?.doctorCode
                                val hospitalId = binding.etHospitalId.text.toString().trim()

                                if (token != null) {
                                    val tokenManager = TokenManager(this@LoginActivity)
                                    tokenManager.saveAuthData(token, "DOCTOR")
                                    if (hospitalId.isNotEmpty()) tokenManager.saveHospitalId(hospitalId)
                                    if (!doctorCode.isNullOrEmpty()) {
                                        tokenManager.saveDoctorCode(doctorCode)
                                    }

                                    Toast.makeText(this@LoginActivity, "Login Successful!", Toast.LENGTH_SHORT).show()

                                    // Route directly to DoctorMainActivity (which will open the Department fragment by default)
                                    val intent = Intent(this@LoginActivity, DoctorMainActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                } else {
                                    Toast.makeText(this@LoginActivity, "Authentication token missing", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                }

            }
        }
    }
}
package com.swathi.queue_app.v2.ui.auth

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.swathi.queue_app.databinding.SignupBinding
import com.swathi.queue_app.v2.models.SignupRequest
import com.swathi.queue_app.v2.viewmodels.AuthViewModel
import com.swathi.queue_app.v2.viewmodels.Resource
import kotlinx.coroutines.launch



class SignupActivity : AppCompatActivity() {

    private lateinit var binding: SignupBinding
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

            // Trigger the ViewModel function instead of direct networking
            authViewModel.signup(request)
        }
    }

    private fun observeSignupState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.signupState.collect { resource ->
                    if (resource == null) return@collect

                    if (resource is Resource.Loading) {
                        binding.btnSignup.isEnabled = false
                    }
                    else if (resource is Resource.Error) {
                        binding.btnSignup.isEnabled = true
                        Log.d("SignUpActivity", "${resource.message}")
                        Toast.makeText(this@SignupActivity, "Registration Failed: ${resource.message}", Toast.LENGTH_LONG).show()
                    }
                    else if (resource is Resource.Success<*>) {
                        binding.btnSignup.isEnabled = true
                        Toast.makeText(this@SignupActivity, "Registration Successful!", Toast.LENGTH_SHORT).show()
                        finish() // Close activity and head back to login
                    }
                }
            }
        }
    }
}
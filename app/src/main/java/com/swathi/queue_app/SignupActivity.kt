package com.swathi.queue_app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.swathi.queue_app.databinding.SignupBinding
import com.swathi.queue_app.viewmodel.AuthViewModel

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: SignupBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = SignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSignup.setOnClickListener {
            Toast.makeText(
                this,
                "Signup Clicked",
                Toast.LENGTH_SHORT
            ).show()
            val name = binding.etname.text.toString()
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()
if(name.isEmpty()||email.isEmpty()||password.isEmpty()){
    Toast.makeText(this,"Enter all fields", Toast.LENGTH_SHORT).show()
    return@setOnClickListener
}
            viewModel.signup(
                name,
                email,
                password
            )
        }



        viewModel.signupResponse.observe(this) {

            Toast.makeText(
                this,
                it.message,
                Toast.LENGTH_SHORT
            ).show()
            Toast.makeText(
                this,
                "Please login",
                Toast.LENGTH_SHORT
            ).show()
            startActivity(
                Intent(
                    this,
                    loginactivity::class.java
                )
            )

            finish()
        }
        viewModel.errorResponse.observe(this){

            Toast.makeText(
                this,
                "Account already exists, please log in",
                Toast.LENGTH_LONG
            ).show()

            startActivity(Intent(this, loginactivity::class.java))
        }
        binding.tvlogin.setOnClickListener {

            startActivity(Intent(this, loginactivity::class.java))
        }
    }
}
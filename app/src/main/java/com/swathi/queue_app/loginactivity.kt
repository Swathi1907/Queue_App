package com.swathi.queue_app

import android.os.Bundle
import android.content.Intent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.messaging.FirebaseMessaging
import com.swathi.queue_app.databinding.LoginBinding
import com.swathi.queue_app.viewmodel.AuthViewModel
class loginactivity : AppCompatActivity() { // create a screen called main activity
    private lateinit var binding: LoginBinding
    private val viewModel: AuthViewModel by viewModels() // creates viewmodel
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        binding= LoginBinding.inflate(layoutInflater) // connects to the login xml screen
         setContentView(binding.root)// shows the xml on the screen

        binding.btnLogin.setOnClickListener {
            println("LOGIN BUTTON CLICKED")
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()
            viewModel.login(
                email,
                password

            )

        }
        val sharedPref = getSharedPreferences("QueuePrefs", MODE_PRIVATE)

        val token = sharedPref.getString("token", null)

        if (token != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
// login response is live data so we need to observe it all the time for tbe updates
        viewModel.loginresponse.observe(this) {

            val prefs =
                getSharedPreferences(
                    "app",
                    MODE_PRIVATE
                )

            prefs.edit()
                .putString(
                    "jwt_token",
                    it.jwt_token
                )
                .apply()
            println("JWT_TOKEN: ${it.jwt_token}")
            println("OBSERVER CALLED")
            println(it.name)

            Toast.makeText(
                this,
                "Welcome ${it.name}",
                Toast.LENGTH_LONG
            ).show()
            if(it.role == "admin") {

                startActivity(
                    Intent(
                        this,
                        AdminActivity::class.java
                    )
                )

            } else {

                startActivity(
                    Intent(
                        this,
                        MainActivity::class.java
                    )
                )
            }
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token ->

                    viewModel.saveFcmToken(token)

                }
            finish()
        }

    }

}
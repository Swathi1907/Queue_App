package com.swathi.queue_app

import android.os.Bundle
import android.content.Intent
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
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
        val prefs = getSharedPreferences("app", MODE_PRIVATE)

        val jwtToken = prefs.getString("jwt_token", null)
        val role = prefs.getString("role", null)

        if (jwtToken != null) {

            SocketManager.connect()

            if (role == "admin") {
                startActivity(Intent(this, AdminActivity::class.java))
            } else {
                startActivity(Intent(this, MainActivity::class.java))
            }

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
                .putString("role", it.role)
                .apply()
            println("JWT_TOKEN: ${it.jwt_token}")
            println("OBSERVER CALLED")
            println(it.name)

            Toast.makeText(
                this,
                "Welcome ${it.name}",
                Toast.LENGTH_LONG
            ).show()
            SocketManager.connect()
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token ->

                    viewModel.saveFcmToken(token)

                }
            if (it.role == "admin") {

            showHospitalDialog()

        } else {

            startActivity(
                Intent(this, MainActivity::class.java)
            )

            finish()
        }


        }
        viewModel.verifyHospitalResponse.observe(this) {

            val prefs =
                getSharedPreferences(
                    "app",
                    MODE_PRIVATE
                )

            prefs.edit()
                .putString(
                    "hospitalId",
                    it.hospitalId
                )
                .putString(
                    "hospitalName",
                    it.hospitalName
                )
                .apply()
            Log.d("LOGIN", "Saving hospitalId = ${it.hospitalId}")
            Toast.makeText(
                this,
                "Welcome to ${it.hospitalName}",
                Toast.LENGTH_SHORT
            ).show()

            startActivity(
                Intent(
                    this,
                    AdminActivity::class.java
                )
            )

            finish()
        }

    }
    private fun showHospitalDialog() {

        val view = layoutInflater.inflate(
            R.layout.dialog_hospital_id,
            null
        )

        val editText = view.findViewById<TextInputEditText>(
            R.id.etHospitalId
        )

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Hospital Verification")
            .setMessage("Enter your Hospital ID to continue.")
            .setView(view)
            .setCancelable(false)
            .setNegativeButton("Logout") { _, _ ->
                finish()
            }
            .setPositiveButton("Verify", null)
            .create()

        dialog.setOnShowListener {

            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {

                    val hospitalId =
                        editText.text.toString().trim()

                    if (hospitalId.isEmpty()) {

                        editText.error = "Hospital ID is required"
                        return@setOnClickListener
                    }

                    viewModel.verifyHospital(hospitalId)
                }
        }

        dialog.show()
    }

}
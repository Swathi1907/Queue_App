package com.swathi.queue_app.v2.Activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.swathi.queue_app.R
import com.swathi.queue_app.databinding.DoctorMainAcitvityBinding

class DoctorMainActivity : AppCompatActivity() {

    private lateinit var binding: DoctorMainAcitvityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DoctorMainAcitvityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.doctorfragmentContainerView) as NavHostFragment
        val navController = navHostFragment.navController

        // Connect bottom navigation with nav controller
        binding.bottomNavView.setupWithNavController(navController)

        // Handle session routing override if sent from LoginActivity
        if (intent.getStringExtra("NAVIGATE_TO") == "HOME" && savedInstanceState == null) {
            navController.navigate(R.id.doctorHomeFragment)
        }
    }
}
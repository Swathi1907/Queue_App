package com.swathi.queue_app.v2.Activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupActionBarWithNavController
import com.swathi.queue_app.R
import com.swathi.queue_app.databinding.ActivityCompounderBinding

class CompounderMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCompounderBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCompounderBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // 1. Set the toolbar as the activity's action bar
        setSupportActionBar(binding.toolbar)
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.fragmentContainer) as NavHostFragment
        navController = navHostFragment.navController

        setupActionBarWithNavController(navController)
    }

    // Enables back button support in the action bar if you use one
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
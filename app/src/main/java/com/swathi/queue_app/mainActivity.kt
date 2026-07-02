package com.swathi.queue_app

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.swathi.queue_app.databinding.ActivityMainBinding
import com.swathi.queue_app.fragments.HomeFragment
import com.swathi.queue_app.fragments.myqueuefragment
import com.swathi.queue_app.fragments.ProfileFragment
import com.swathi.queue_app.viewmodel.HomeViewModel
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val homeViewModel: HomeViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.fragmentContainer,
                    HomeFragment()
                )
                .commit()
        }



        homeViewModel.activeQueueResponse.observe(this) { response ->

            if (response.active) {

                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, myqueuefragment())
                    .commit()

            } else {

                MaterialAlertDialogBuilder(this)
                    .setTitle("No Active Queue")
                    .setMessage("You're not currently in any queue.\n\nJoin a queue from the Home screen.")
                    .setPositiveButton("OK") { _, _ ->
                        binding.bottomNav.selectedItemId = R.id.homeFragment
                    }
                    .show()
            }
        }
        binding.bottomNav.setOnItemSelectedListener {

            when (it.itemId) {

                R.id.homeFragment -> {

                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, HomeFragment())
                        .commit()
                    true
                }

                R.id.myQueueFragment -> {

                    homeViewModel.getMyActiveQueue()
                    true
                }

                R.id.profileFragment -> {

                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, ProfileFragment())
                        .commit()
                    true
                }

                else -> false
            }
        }

            true
        }
    }

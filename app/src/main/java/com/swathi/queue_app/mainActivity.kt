package com.swathi.queue_app

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.swathi.queue_app.databinding.ActivityMainBinding
import com.swathi.queue_app.fragments.HomeFragment
import com.swathi.queue_app.fragments.myqueuefragment
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
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

        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.e("FCM", "Failed", task.exception)
                    return@addOnCompleteListener
                }

                Log.d("FCM", task.result)
            }
        homeViewModel.activeQueueResponse.observe(this) { queues ->

            if (queues.isNotEmpty()) {

                val activeQueue = queues.first()

                val fragment = myqueuefragment().apply {
                    arguments = Bundle().apply {
                        putString("queueId", activeQueue.queueId)
                    }
                }

                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
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
     /*   homeViewModel.activeQueueResponse.observe(this) { response ->

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
        } */
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

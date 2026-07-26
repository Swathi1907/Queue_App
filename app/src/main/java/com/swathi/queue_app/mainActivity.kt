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
import com.swathi.queue_app.fragments.NotificationBottomSheet
import com.swathi.queue_app.fragments.ProfileFragment
import com.swathi.queue_app.viewmodel.HomeViewModel
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val homeViewModel: HomeViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        homeViewModel.getNotificationCount()
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
     /*   homeViewModel.activeQueueResponse.observe(this) { queues ->

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
        } */
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
        homeViewModel.notificationCount.observe(this) { response ->

            val badge = binding.bottomNav.getOrCreateBadge(R.id.notificationFragment)

            if (response.count == 0) {
                badge.isVisible = false
            } else {
                badge.isVisible = true
                badge.number = response.count
            }
        }
        homeViewModel.readNotificationResponse.observe(this) {
            homeViewModel.getNotificationCount()
        }
        binding.bottomNav.setOnItemSelectedListener {

            when (it.itemId) {

                R.id.homeFragment -> {

                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, HomeFragment())
                        .commit()
                    true
                }
                R.id.notificationFragment -> {

                    NotificationBottomSheet()
                        .show(supportFragmentManager, "NotificationBottomSheet")

                    homeViewModel.markNotificationsRead()
                    // Reset selection to Home (or the previously selected tab)
                //    binding.bottomNav.selectedItemId = R.id.homeFragment

                    true
                }
          /*      R.id.myQueueFragment -> {

                    homeViewModel.getMyActiveQueue()
                    true
                } */

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
    override fun onResume() {
        super.onResume()
        homeViewModel.getNotificationCount()
    }
    }

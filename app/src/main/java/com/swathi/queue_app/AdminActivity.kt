package com.swathi.queue_app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.swathi.queue_app.databinding.AdminMainBinding
import com.swathi.queue_app.fragments.admin.DashboardFragment
import androidx.fragment.app.Fragment
import com.swathi.queue_app.fragments.ProfileFragment
import com.swathi.queue_app.fragments.admin.QueuesFragment

class AdminActivity : AppCompatActivity() {

    private lateinit var binding: AdminMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding =
            AdminMainBinding.inflate(
                layoutInflater
            )

        setContentView(binding.root)

        replaceFragment(
            DashboardFragment()
        )

        binding.bottomNavigation.setOnItemSelectedListener {

            when (it.itemId) {

                R.id.nav_dashboard -> {

                    replaceFragment(
                        DashboardFragment()
                    )
                    true
                }

                R.id.nav_queues -> {

                    replaceFragment(
                        QueuesFragment()
                    )
                    true
                }

                R.id.nav_profile -> {

                    replaceFragment(
                        ProfileFragment()
                    )
                    true
                }

                else -> false
            }
        }
    }

    private fun replaceFragment(
        fragment: Fragment
    ) {

        supportFragmentManager
            .beginTransaction()
            .replace(
                R.id.adminFragmentContainer,
                fragment
            )
            .commit()
    }
}
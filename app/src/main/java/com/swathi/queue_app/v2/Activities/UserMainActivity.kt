package com.swathi.queue_app.v2.Activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.swathi.queue_app.R
import com.swathi.queue_app.databinding.UserMainActivityBinding
import com.swathi.queue_app.v2.fragments.user.HomeFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: UserMainActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate layout using ViewBinding (ensure you have activity_main.xml layout created)
        binding = UserMainActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load HomeFragment by default on initial creation
        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
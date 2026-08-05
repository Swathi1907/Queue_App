package com.swathi.queue_app.v2.fragments.user

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.swathi.queue_app.databinding.UserProfileBinding // Ensure matches your layout filename
import com.swathi.queue_app.v2.ui.auth.LoginActivity
import com.swathi.queue_app.v2.utilis.TokenManager // Adjust package as needed

class ProfileFragment : Fragment() {

    private var _binding: UserProfileBinding ? = null
    private val binding get() = _binding!!

    private lateinit var tokenManager: TokenManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding =UserProfileBinding .inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tokenManager = TokenManager(requireContext())

        loadUserData()
        setupListeners()
    }

    private fun loadUserData() {
        // Retrieve stored user information from TokenManager or local session
        val userName = tokenManager.getUserName() ?: "Alex Morgan"
        val userEmail = tokenManager.getEmail() ?: "alex.morgan@example.com"
        val userPhone = tokenManager.getContact() ?: "+1 (555) 348-9120"

        binding.tvUserName.text = userName
        binding.tvUserEmail.text = userEmail
        binding.tvUserPhone.text = userPhone

        // App Version is hardcoded in XML or can be set dynamically here
        binding.tvPreferencesValue.text = "2.0.0 (mega)"
    }

    private fun setupListeners() {
        binding.btnEditProfile.setOnClickListener {
            Toast.makeText(requireContext(), "Edit Profile clicked", Toast.LENGTH_SHORT).show()
        }

        binding.btnSettings.setOnClickListener {
            Toast.makeText(requireContext(), "Settings clicked", Toast.LENGTH_SHORT).show()
        }

        binding.layoutLogout.setOnClickListener {
            // Clear local session token
            tokenManager.clearSession()
            Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()

            // Navigate to LoginActivity and clear the back stack
            val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            requireActivity().finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
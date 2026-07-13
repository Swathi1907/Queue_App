package com.swathi.queue_app.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.swathi.queue_app.SocketManager
import com.swathi.queue_app.databinding.ProfileBinding
import com.swathi.queue_app.loginactivity
import com.swathi.queue_app.viewmodel.AuthViewModel
import kotlin.getValue

class ProfileFragment : Fragment() {

    private var _binding: ProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = ProfileBinding.inflate(
            inflater,
            container,
            false
        )

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(
            view,
            savedInstanceState
        )



        viewModel.getProfile()

        viewModel.profileResponse.observe(
            viewLifecycleOwner
        ) {

            binding.tvName.text = it.name
            binding.tvEmail.text = it.email
            binding.tvRole.text = it.role
        }
        binding.cardSettings.setOnClickListener {

            // Open Settings Fragment later
        }

        binding.cardAbout.setOnClickListener {

            // Open About Dialog later
        }

        binding.btnlogout.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout") { _, _ ->
                    SocketManager.disconnect()
                    val sharedPref = requireActivity()
                        .getSharedPreferences("app", Context.MODE_PRIVATE)

                    sharedPref.edit().clear().apply()

                    val intent = Intent(requireContext(), loginactivity::class.java)
                    intent.flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

                    startActivity(intent)
                    requireActivity().finish()
                }
                .setNegativeButton("Cancel", null)
                .show()

        }}


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
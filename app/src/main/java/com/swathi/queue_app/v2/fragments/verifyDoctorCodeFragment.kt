package com.swathi.queue_app.v2.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.swathi.queue_app.databinding.FragmentVerifyDoctorCodeBinding
import com.swathi.queue_app.v2.utilis.TokenManager
import com.swathi.queue_app.v2.viewmodels.AuthViewModel

class VerifyDoctorCodeFragment : Fragment() {

    private var _binding: FragmentVerifyDoctorCodeBinding? = null
    private val binding get() = _binding!!

    // Use property delegation with val and correct import (androidx.fragment.app.viewModels)
    private val authViewModel: AuthViewModel by viewModels()

    // Retrieve safe args if generated, or fallback to standard bundle extraction


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVerifyDoctorCodeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // You can get the doctorId safely via Safe Args
      val doctorId = arguments?.getString("doctorId")

        binding.btnVerify.setOnClickListener {
            val code = binding.etVerificationCode.text.toString().trim()
            if (code.isEmpty()) {
                binding.tilVerificationCode.error = "Please enter the verification code"
                return@setOnClickListener
            }

            binding.tilVerificationCode.error = null

            // Trigger your backend verification through the ViewModel here
            // authViewModel.verifyDoctorCode(doctorId, code)
// Inside VerifyDoctorCodeFragment.kt
            val tokenManager = TokenManager(requireContext())
            val email = tokenManager.getEmail() ?: ""
            val password = tokenManager.getPassword() ?: ""
            val hospitalId = tokenManager.getHospitalId() ?: ""

            if (email.isEmpty() || password.isEmpty() || hospitalId.isEmpty()) {
                Toast.makeText(requireContext(), "Session details missing. Please login again.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val doctorId = arguments?.getString("doctorId") ?: ""

            if (doctorId.isEmpty()) {
                Toast.makeText(requireContext(), "Doctor ID missing", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            authViewModel.verifyDoctorCode(email, password, hospitalId, doctorId)
            Toast.makeText(requireContext(), "Verifying ID: $doctorId", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
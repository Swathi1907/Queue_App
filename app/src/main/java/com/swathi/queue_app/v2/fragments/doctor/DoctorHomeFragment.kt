package com.swathi.queue_app.v2.fragments.doctor

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.swathi.queue_app.R
import com.swathi.queue_app.databinding.FragmentDoctorHomeBinding
import com.swathi.queue_app.v2.adapter.doctor.NextMembersAdapter
import com.swathi.queue_app.v2.models.SessionData
import com.swathi.queue_app.v2.viewmodels.Queueviewmodel
import com.swathi.queue_app.v2.viewmodels.Resource
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DoctorHomeFragment : Fragment(R.layout.fragment_doctor_home) {
    private val tokenManager by lazy { com.swathi.queue_app.v2.utilis.TokenManager(requireContext()) }
    private val viewModel: Queueviewmodel by viewModels()
    private var _binding: FragmentDoctorHomeBinding? = null
    private val binding get() = _binding!!
    private var department: String? = null
    private var doctorCode: String? = null

    private val nextMembersAdapter by lazy { NextMembersAdapter() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDoctorHomeBinding.bind(view)
        department = arguments?.getString("DEPARTMENT") ?: ""
        doctorCode = arguments?.getString("DOCTOR_CODE") ?: ""
        Log.d("dhf", "received ${doctorCode}")

        setupRecyclerView()

        if (!department.isNullOrEmpty() && !doctorCode.isNullOrEmpty()) {
            viewModel.fetchActiveSession(department!!, doctorCode!!)
        } else {
            Toast.makeText(requireContext(), "Missing department or doctor code", Toast.LENGTH_SHORT).show()
        }

        binding.back.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // Action button when active session exists (Complete / Call Next)
        binding.btnCompleteNext.setOnClickListener {
            if (department.isNullOrEmpty() || doctorCode.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Missing department or doctor code", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val buttonText = binding.btnCompleteNext.text.toString().trim()
            if (buttonText.equals("Call Next", ignoreCase = true)) {
                viewModel.callNextPatient(department!!, doctorCode!!)
            } else if (buttonText.startsWith("Complete", ignoreCase = true)) {
                viewModel.completeConsultation(department!!, doctorCode!!)
            }
        }

        // Action button when NO active session exists (Start New Session)
        binding.btnStartNewSession.setOnClickListener {
            if (department.isNullOrEmpty() || doctorCode.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Missing department or doctor code", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Retrieve hospitalId from your session/token manager (adjust based on how you store it)
            val hospitalId = tokenManager.getHospitalId() ?: "" // Or wherever you save it during login

            if (hospitalId.isEmpty()) {
                Toast.makeText(requireContext(), "Hospital ID missing. Please log in again.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.createQueue(hospitalId, department!!, doctorCode!!)
        }

        observeQueueState()
    }

    private fun setupRecyclerView() {
        binding.rvUpNext.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = nextMembersAdapter
        }
    }

    private fun observeQueueState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.queueState.collectLatest { resource ->
                    when (resource) {
                        is Resource.Loading -> {}
                        is Resource.Success -> {
                            val data = resource.data
                            when {
                                data is SessionData -> {
                                    updateSessionUI(data)
                                }
                                data is String -> {
                                    Toast.makeText(requireContext(), data, Toast.LENGTH_SHORT).show()
                                    // Automatically fetch the active session data after a successful queue creation or action message
                                    if (!department.isNullOrEmpty() && !doctorCode.isNullOrEmpty()) {
                                        viewModel.fetchActiveSession(department!!, doctorCode!!)
                                    }
                                }
                                data != null -> {
                                    Log.d("dhf", "Unexpected data type received: ${data::class.java.name}")
                                }
                                else -> {
                                    updateSessionUI(null)
                                }
                            }
                        }
                        is Resource.Error -> {
                            Log.d("dhf", "${resource.message}")

                            // If it's just an empty queue warning, don't destroy the active session view
                            if (resource.message?.contains("No waiting patients", ignoreCase = true) == true) {
                                Toast.makeText(requireContext(), resource.message, Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(requireContext(), resource.message, Toast.LENGTH_LONG).show()
                                updateSessionUI(null)
                            }
                        }
                        null -> {
                            updateSessionUI(null)
                        }
                    }
                }
            }
        }
    }
    private fun updateSessionUI(sessionData: SessionData?) {
        // If there is no session at all, show the start new session layout
        if (sessionData == null) {
            binding.layoutNoSession.visibility = View.VISIBLE
            binding.layoutActiveSessionGroup.visibility = View.GONE
            nextMembersAdapter.submitList(emptyList())
            return
        }

        // Session exists: show active layout, hide empty layout
        binding.layoutNoSession.visibility = View.GONE
        binding.layoutActiveSessionGroup.visibility = View.VISIBLE

        // Bind queue status
        binding.badgeStatus.text = "● ${sessionData.queueStatus ?: "Active"}"

        // Find the token currently in consultation
        val activeToken = sessionData.tokens?.find { it.status == "IN_CONSULTATION" }

        if (activeToken != null) {
            binding.tvTokenNumber.text = activeToken.tokenNumber ?: "---"
            binding.tvPatientName.text = activeToken.patientName ?: "Unknown Patient"
            binding.tvConsultDetails.text = activeToken.notes ?: "Active consultation session in progress."
            binding.btnCompleteNext.text = "Complete & Next"
        } else {
            binding.tvTokenNumber.text = "---"
            binding.tvPatientName.text = "No patient is in consultation"
            binding.tvConsultDetails.text = "Queue is active, waiting for next patient."
            binding.btnCompleteNext.text = "Call Next"
        }

        // Filter waiting tokens safely
        val waitingTokens = sessionData.tokens?.filter {
            it.status == "WAITING" || it.status == "PENDING"
        } ?: emptyList()

        nextMembersAdapter.submitList(waitingTokens)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
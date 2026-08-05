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
import com.swathi.queue_app.v2.adapter.doctor.NextMembersAdapter // Ensure correct package import
import com.swathi.queue_app.v2.models.SessionData
import com.swathi.queue_app.v2.viewmodels.Queueviewmodel
import com.swathi.queue_app.v2.viewmodels.Resource
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DoctorHomeFragment : Fragment(R.layout.fragment_doctor_home) {

    private val viewModel: Queueviewmodel by viewModels()
    private var _binding: FragmentDoctorHomeBinding? = null
    private val binding get() = _binding!!
    private var department: String? = null
    private var doctorCode: String? = null

    // 1. Declare the adapter instance lazily
    private val nextMembersAdapter by lazy { NextMembersAdapter() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDoctorHomeBinding.bind(view)
        department = arguments?.getString("DEPARTMENT") ?: ""
        doctorCode = arguments?.getString("DOCTOR_CODE") ?: ""
        Log.d("dhf", "received ${doctorCode}")

        // 2. Setup the RecyclerView layout manager and adapter connection
        setupRecyclerView()

        if (!department.isNullOrEmpty() && !doctorCode.isNullOrEmpty()) {
            viewModel.fetchActiveSession(department!!, doctorCode!!)
        } else {
            Toast.makeText(requireContext(), "Missing department or doctor code", Toast.LENGTH_SHORT).show()
        }
        binding.btnCompleteNext.setOnClickListener {
            if (department.isNullOrEmpty() || doctorCode.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Missing department or doctor code", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val buttonText = binding.btnCompleteNext.text.toString().trim()
            if (buttonText.equals("Call Next", ignoreCase = true)) {
                viewModel.callNextPatient(department!!, doctorCode!!)
            } else if (buttonText.equals("Complete", ignoreCase = true)) {
                viewModel.completeConsultation(department!!, doctorCode!!)
            }
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
                                }
                                data != null -> {
                                    Log.d("dhf", "Unexpected data type received: ${data::class.java.name}")
                                }
                                else -> {
                                    Log.d("dhf", "Resource data is null")
                                }
                            }
                        }
                        is Resource.Error -> {
                            Log.d("dhf", "${resource.message}")
                            Toast.makeText(requireContext(), resource.message, Toast.LENGTH_LONG).show()
                        }
                        null -> {}
                    }
                }
            }
        }
    }

    private fun updateSessionUI(sessionData: SessionData) {
        // Bind queue status
        binding.badgeStatus.text = "● ${sessionData.queueStatus}"

        // Find the token currently in consultation from the tokens array
        val activeToken = sessionData.tokens?.find { it.status == "IN_CONSULTATION" }

        if (activeToken != null) {
            binding.tvTokenNumber.text = activeToken.tokenNumber ?: "---"
            binding.tvPatientName.text = activeToken.patientName ?: "Unknown Patient"
            binding.tvConsultDetails.text = activeToken.notes ?: "Active consultation session in progress."
            binding.btnCompleteNext.text="Complete"
        } else {
            binding.tvTokenNumber.text = "---"
            binding.tvPatientName.text = "No patient is in consultation"
            binding.tvConsultDetails.text = "Queue is active, waiting for next patient."
            binding.btnCompleteNext.text="Call Next "
        }

        // 3. Filter waiting tokens and submit them using the adapter instance
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
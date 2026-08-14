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
    private var isExpanded = false
    private var lastKnownSessionData: SessionData? = null

    private val nextMembersAdapter by lazy { NextMembersAdapter() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDoctorHomeBinding.bind(view)
        department = arguments?.getString("DEPARTMENT") ?: ""
        doctorCode = arguments?.getString("DOCTOR_CODE") ?: ""
        Log.d("dhf", "received ${doctorCode}")

        // Initialize RecyclerView properly
        setupRecyclerView()

        if (!department.isNullOrEmpty() && !doctorCode.isNullOrEmpty()) {
            viewModel.fetchActiveSession(department!!, doctorCode!!)
        } else {
            Toast.makeText(requireContext(), "Missing department or doctor code", Toast.LENGTH_SHORT).show()
        }

        binding.back.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // View All / Show Less Expandable Click Listener (Correctly placed outside other clicks)

// View All / Show Less Expandable Click Listener
        binding.tvViewAll.setOnClickListener {
            isExpanded = !isExpanded
            val layoutParams = binding.rvUpNext.layoutParams

            if (isExpanded) {
                layoutParams.height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                binding.tvViewAll.text = "Show Less"
            } else {
                // Fallback to a fixed pixel size or wrap content if dimension is missing
                layoutParams.height = try {
                    resources.getDimensionPixelSize(R.dimen.collapsed_recycler_height)
                } catch (e: Exception) {
                    400 // Fallback height in pixels if dimen is not found
                }
                binding.tvViewAll.text = "View All"
            }
            binding.rvUpNext.layoutParams = layoutParams
            binding.rvUpNext.requestLayout() // Force the view to redraw with the new height
        }
        // Action button when active session exists (Complete / Call Next)
        binding.btnCompleteNext.setOnClickListener {
            if (department.isNullOrEmpty() || doctorCode.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Missing department or doctor code", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check if queue is paused before letting them trigger actions
            val sessionData = (viewModel.queueState.value as? Resource.Success<*>)?.data as? SessionData
            if (sessionData?.queueStatus.equals("PAUSED", ignoreCase = true)) {
                Toast.makeText(requireContext(), "Cannot proceed while queue is paused", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val buttonText = binding.btnCompleteNext.text.toString().trim()
            if (buttonText.equals("Call Next", ignoreCase = true)) {
                viewModel.callNextPatient(department!!, doctorCode!!)
            } else if (buttonText.startsWith("Complete", ignoreCase = true)) {
                viewModel.completeConsultation(department!!, doctorCode!!)
            }
        }

        // Action for pausing or resuming the queue
        binding.btnPauseResume.setOnClickListener {
            if (department.isNullOrEmpty() || doctorCode.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Missing department or doctor code", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val currentStatus = binding.badgeStatus.text.toString()
            val newStatus = if (currentStatus.contains("PAUSED", ignoreCase = true)) "ACTIVE" else "PAUSED"

            viewModel.updateQueueStatus(department!!, doctorCode!!, newStatus)
        }

        // Action button when NO active session exists (Start New Session)
        binding.btnStartNewSession.setOnClickListener {
            if (department.isNullOrEmpty() || doctorCode.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Missing department or doctor code", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val hospitalId = tokenManager.getHospitalId() ?: ""
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
                        is Resource.Loading -> {
                            // Optional: Show a subtle progress indicator if desired
                        }
                        is Resource.Success -> {
                            val data = resource.data
                            when (data) {
                                is SessionData -> {
                                    lastKnownSessionData = data // Cache the active session state
                                    updateSessionUI(data)
                                }
                                is String -> {
                                    Toast.makeText(requireContext(), data, Toast.LENGTH_SHORT).show()
                                    if (!department.isNullOrEmpty() && !doctorCode.isNullOrEmpty()) {
                                        viewModel.fetchActiveSession(department!!, doctorCode!!)
                                    }
                                }
                                else -> {
                                    if (lastKnownSessionData == null) {
                                        updateSessionUI(null)
                                    }
                                }
                            }
                        }
                        is Resource.Error -> {
                            Log.d("dhf", "${resource.message}")
                            Toast.makeText(requireContext(), resource.message, Toast.LENGTH_LONG).show()

                            // FIX: If we have a cached session, ALWAYS keep showing it on action/network errors.
                            // Do not wipe the screen or reset lastKnownSessionData to null!
                            if (lastKnownSessionData != null) {
                                updateSessionUI(lastKnownSessionData)
                            } else {
                                updateSessionUI(null)
                            }
                        }
                        null -> {
                            if (lastKnownSessionData == null) {
                                updateSessionUI(null)
                            }
                        }
                    }
                }
            }
        }
    }
    private fun updateSessionUI(sessionData: SessionData?) {
        if (sessionData == null || (sessionData.sessionId.isNullOrEmpty() && (sessionData.tokens.isNullOrEmpty() || sessionData.queueStatus.isNullOrEmpty()))) {
            binding.layoutNoSession.visibility = View.VISIBLE
            binding.layoutActiveSessionGroup.visibility = View.GONE
            nextMembersAdapter.submitList(emptyList())
            return
        }

        if (sessionData.queueStatus.equals("CLOSED", ignoreCase = true)) {
            binding.layoutNoSession.visibility = View.VISIBLE
            binding.layoutActiveSessionGroup.visibility = View.GONE
            nextMembersAdapter.submitList(emptyList())
            return
        }

        binding.layoutNoSession.visibility = View.GONE
        binding.layoutActiveSessionGroup.visibility = View.VISIBLE

        val isPaused = sessionData.queueStatus.equals("PAUSED", ignoreCase = true)
        binding.badgeStatus.text = "● ${sessionData.queueStatus ?: "Active"}"

        // --- UPDATE STATS CARDS ---
        val waitingTokens = sessionData.tokens?.filter {
            it.status == "WAITING" || it.status == "PENDING"
        } ?: emptyList()

        // Set Total in Queue Count
        binding.tvStatStatusValue.text = waitingTokens.size.toString()

        // Set Average Service Time (defaults to 5 mins if null)
        val avgTime = sessionData.avgServiceTime ?: 5
        binding.tvAvgServiceTime.text = "$avgTime mins"
        // --------------------------

        if (isPaused) {
            binding.btnPauseResume.text = "Resume"
            binding.btnCompleteNext.alpha = 0.5f
            binding.tvTokenNumber.text = "---"
            binding.tvPatientName.text = "Queue is Paused"
            binding.tvConsultDetails.text = "Resume the queue to continue consultations."
            binding.btnCompleteNext.text = "Call Next"
        } else {
            binding.btnPauseResume.text = "Pause"
            binding.btnCompleteNext.alpha = 1.0f

            val activeToken = sessionData.tokens?.find { it.status == "IN_CONSULTATION" }
            if (activeToken != null) {
                binding.tvTokenNumber.text = activeToken.tokenNumber ?: "---"
                binding.tvPatientName.text = activeToken.patientName ?: "Unknown Patient"
                binding.tvConsultDetails.text = if (!activeToken.notes.isNullOrEmpty()) "🩺 ${activeToken.notes}" else "Active consultation session in progress."
                binding.btnCompleteNext.text = "Complete"
            } else {
                binding.tvTokenNumber.text = "---"
                binding.tvPatientName.text = "No patient is in consultation"
                binding.tvConsultDetails.text = "Queue is active, waiting for next patient."
                binding.btnCompleteNext.text = "Call Next"
            }
        }

        if (waitingTokens.isEmpty()) {
            binding.rvUpNext.visibility = View.GONE
            binding.tvEmptyQueue.visibility = View.VISIBLE
            binding.tvViewAll.visibility = View.GONE
        } else {
            binding.rvUpNext.visibility = View.VISIBLE
            binding.tvEmptyQueue.visibility = View.GONE
            binding.tvViewAll.visibility = View.VISIBLE
        }

        nextMembersAdapter.submitList(waitingTokens)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
package com.swathi.queue_app.v2.fragments.doctor

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import kotlin.math.roundToInt
import com.swathi.queue_app.v2.utilis.SocketManager
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.swathi.queue_app.R
import com.swathi.queue_app.databinding.FragmentDoctorHomeBinding
import com.swathi.queue_app.v2.adapter.doctor.NextMembersAdapter
import com.swathi.queue_app.v2.models.SessionData
import com.swathi.queue_app.v2.viewmodels.DoctorViewModel
import com.swathi.queue_app.v2.viewmodels.Queueviewmodel
import com.swathi.queue_app.v2.viewmodels.Resource
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DoctorHomeFragment : Fragment(R.layout.fragment_doctor_home) {
    private val tokenManager by lazy { com.swathi.queue_app.v2.utilis.TokenManager(requireContext()) }
    private val viewModel: Queueviewmodel by viewModels()
    private val viewmodel: DoctorViewModel by viewModels()
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
binding.btnEndSession.setOnClickListener {
    Toast.makeText(requireContext(),"clicked",Toast.LENGTH_SHORT).show()
    if (department.isNullOrEmpty() || doctorCode.isNullOrEmpty()) {
        Toast.makeText(
            requireContext(),
            "Missing department or doctor code",
            Toast.LENGTH_SHORT
        ).show()
        return@setOnClickListener
    }

    viewmodel.endSession(
        department!!,
        doctorCode!!
    )
}
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
        observeEndSession()
        observeQueueState()
        setupSocketListeners()
    }

    private fun setupRecyclerView() {
        binding.rvUpNext.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = nextMembersAdapter
        }
    }
    private fun observeEndSession() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewmodel.doctorEndSession.collectLatest { resource ->
                    when (resource) {
                        is Resource.Loading -> {
                            binding.btnEndSession.isEnabled = false
                            binding.btnEndSession.text = "Ending Session..."
                        }

                        is Resource.Success<*> -> {
                            binding.btnEndSession.isEnabled = true
                            binding.btnEndSession.text = "End Session"

                            Toast.makeText(
                                requireContext(),
                                "Session ended successfully",
                                Toast.LENGTH_SHORT
                            ).show() // Fixed missing parentheses here

                            lastKnownSessionData = null
                            updateSessionUI(null) // Immediately clear UI to show no session state

                            if (!department.isNullOrEmpty() && !doctorCode.isNullOrEmpty()) {
                                viewModel.fetchActiveSession(department!!, doctorCode!!)
                            }
                        }

                        is Resource.Error -> {
                            binding.btnEndSession.isEnabled = true
                            binding.btnEndSession.text = "End Session"

                            Toast.makeText(
                                requireContext(),
                                resource.message ?: "Failed to end session",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        else -> {}
                    }
                }
            }
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

                                    lastKnownSessionData = data

                                    updateSessionUI(data)

                                    // Join this doctor's queue Socket.IO room
                                    val queueId = data.sessionId

                                    if (!queueId.isNullOrEmpty()) {

                                        SocketManager.joinQueue(queueId)

                                        Log.d(
                                            "DoctorSocket",
                                            "Joined queue room: queue_$queueId"
                                        )
                                    }
                                }
                                is String -> {
                                    lastKnownSessionData = null

                                    Toast.makeText(
                                        requireContext(),
                                        data,
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    updateSessionUI(null)
                                }
                                null -> {
                                    // Handle null data payload safely after actions like createQueue
                                    lastKnownSessionData = null
                                    updateSessionUI(null)

                                    // If a queue was just created, fetch the active session to load it onto the UI
                                    if (!department.isNullOrEmpty() && !doctorCode.isNullOrEmpty()) {
                                        viewModel.fetchActiveSession(department!!, doctorCode!!)
                                    }
                                }
                                else -> {
                                    // Fallback for any other unexpected type safely without casting crashes
                                    if (lastKnownSessionData == null) {
                                        updateSessionUI(null)
                                    }
                                }
                            }
                        }
                        is Resource.Error -> {
                            Log.d("dhf", "${resource.message}")
                            Toast.makeText(requireContext(), resource.message, Toast.LENGTH_LONG).show()

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
        binding.tvAvgServiceTime.text = "${avgTime.toDouble().toInt()} mins"
        // --------------------------
//"${metrics.avgServiceTime.roundToInt()} min"
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
    private fun setupSocketListeners() {

        // -----------------------------------------
        // PAUSE / RESUME
        // -----------------------------------------
        SocketManager.on("QUEUE_STATUS_CHANGED") { data ->

            val queueId = data.optString("queueId")
            val queueStatus = data.optString("queueStatus")

            Log.d(
                "DoctorSocket",
                "QUEUE_STATUS_CHANGED queue=$queueId status=$queueStatus"
            )

            requireActivity().runOnUiThread {

                if (!isAdded) return@runOnUiThread

                // Refresh the doctor's queue immediately
                if (!department.isNullOrEmpty() &&
                    !doctorCode.isNullOrEmpty()
                ) {

                    viewModel.fetchActiveSession(
                        department!!,
                        doctorCode!!
                    )
                }
            }
        }


        // -----------------------------------------
        // TOKEN COMPLETED
        // -----------------------------------------
        SocketManager.on("TOKEN_COMPLETED") { data ->

            val queueId = data.optString("queueId")
            val tokenNumber = data.optInt("tokenNumber")

            Log.d(
                "DoctorSocket",
                "TOKEN_COMPLETED queue=$queueId token=$tokenNumber"
            )

            requireActivity().runOnUiThread {

                if (!isAdded) return@runOnUiThread

                if (!department.isNullOrEmpty() &&
                    !doctorCode.isNullOrEmpty()
                ) {

                    viewModel.fetchActiveSession(
                        department!!,
                        doctorCode!!
                    )
                }
            }
        }


        // -----------------------------------------
        // NEXT TOKEN CALLED
        // -----------------------------------------
        SocketManager.on("TOKEN_CALLED") { data ->

            val queueId = data.optString("queueId")
            val tokenNumber = data.optInt("tokenNumber")

            Log.d(
                "DoctorSocket",
                "TOKEN_CALLED queue=$queueId token=$tokenNumber"
            )

            requireActivity().runOnUiThread {

                if (!isAdded) return@runOnUiThread

                if (!department.isNullOrEmpty() &&
                    !doctorCode.isNullOrEmpty()
                ) {

                    viewModel.fetchActiveSession(
                        department!!,
                        doctorCode!!
                    )
                }
            }
        }
        // -----------------------------------------
// QUEUE SESSION ENDED
// -----------------------------------------

        SocketManager.on("QUEUE_SESSION_ENDED") { data ->

            val queueId = data.optString("queueId")

            Log.d(
                "DoctorSocket",
                "QUEUE_SESSION_ENDED queue=$queueId"
            )

            requireActivity().runOnUiThread {

                if (!isAdded) return@runOnUiThread

                lastKnownSessionData = null
                updateSessionUI(null)

                Toast.makeText(
                    requireContext(),
                    "Queue session ended",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // -----------------------------------------
// QUEUE CREATED / SESSION STARTED
// -----------------------------------------
        SocketManager.on("QUEUE_CREATED") { data ->

            val queueId = data.optString("queueId")
            val departmentFromSocket = data.optString("department")
            val doctorCodeFromSocket = data.optString("doctorCode")

            Log.d(
                "DoctorSocket",
                "QUEUE_CREATED queue=$queueId department=$departmentFromSocket doctor=$doctorCodeFromSocket"
            )

            requireActivity().runOnUiThread {

                if (!isAdded) return@runOnUiThread

                // Only react if this queue belongs to this doctor
                if (departmentFromSocket == department &&
                    doctorCodeFromSocket == doctorCode
                ) {

                    // Fetch the newly created session
                    viewModel.fetchActiveSession(
                        department!!,
                        doctorCode!!
                    )
                }
            }
        }
    }
    override fun onDestroyView() {

        SocketManager.off("QUEUE_STATUS_CHANGED")
        SocketManager.off("TOKEN_COMPLETED")
        SocketManager.off("TOKEN_CALLED")
        SocketManager.off("QUEUE_SESSION_ENDED")
        SocketManager.off("QUEUE_CREATED")
        super.onDestroyView()
        _binding = null
    }
}
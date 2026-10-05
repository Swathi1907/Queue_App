package com.swathi.queue_app.v2.fragments.user

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast

import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.swathi.queue_app.databinding.NewUserqueueDetailsBinding
import com.swathi.queue_app.v2.models.QueueTicketData
import com.swathi.queue_app.v2.utilis.SocketManager
import com.swathi.queue_app.v2.utilis.TokenManager
import com.swathi.queue_app.v2.viewmodels.Queueviewmodel
import com.swathi.queue_app.v2.viewmodels.Resource

import kotlinx.coroutines.launch


class LiveQueueTicket : Fragment() {

    private var _binding: NewUserqueueDetailsBinding? = null
    private val binding get() = _binding!!

    private lateinit var tokenManager: TokenManager

    private val viewModel: Queueviewmodel by viewModels()

    private var queueId: String? = null

    private var userId: String = ""

    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tokenManager = TokenManager(requireContext())

        queueId = arguments?.getString("Queue_Id")

        userId = tokenManager.getUserId() ?: ""

        Log.d("LiveQueueSocket", "================================")
        Log.d("LiveQueueSocket", "LiveQueueTicket CREATED")
        Log.d("LiveQueueSocket", "Queue ID: $queueId")
        Log.d("LiveQueueSocket", "User ID: $userId")
        Log.d("LiveQueueSocket", "================================")
    }


    // =========================================================
    // CREATE VIEW
    // =========================================================

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = NewUserqueueDetailsBinding.inflate(
            inflater,
            container,
            false
        )

        return binding.root
    }


    // =========================================================
    // VIEW CREATED
    // =========================================================

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()

        observeLiveTicket()

        observeLeaveQueueState()

        setupSocketListeners()


        // =====================================================
        // INITIAL API CALL
        // =====================================================

        if (queueId.isNullOrEmpty()) {

            Log.e(
                "LiveQueueSocket",
                "❌ Queue ID is NULL or EMPTY"
            )

            Toast.makeText(
                requireContext(),
                "Queue ID not found",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (userId.isEmpty()) {

            Log.e(
                "LiveQueueSocket",
                "❌ User ID is NULL or EMPTY"
            )

            Toast.makeText(
                requireContext(),
                "User session not found",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        Log.d(
            "LiveQueueSocket",
            "📡 Calling Live Queue API"
        )

        Log.d(
            "LiveQueueSocket",
            "Queue ID = $queueId"
        )

        Log.d(
            "LiveQueueSocket",
            "User ID = $userId"
        )

        viewModel.fetchLiveTicket(
            queueId!!,
            userId
        )


        // =====================================================
        // JOIN SOCKET ROOM
        // =====================================================

        Log.d(
            "LiveQueueSocket",
            "🔌 Requesting Socket.IO room join"
        )

        Log.d(
            "LiveQueueSocket",
            "Room = queue_$queueId"
        )

        SocketManager.joinQueue(queueId!!)

    }


    // =========================================================
    // SOCKET LISTENERS
    // =========================================================

    private fun setupSocketListeners() {


        // =====================================================
        // ROOM JOIN CONFIRMATION
        // =====================================================

        SocketManager.on("QUEUE_ROOM_JOINED") { data ->

            val joinedQueueId =
                data.optString("queueId")

            val room =
                data.optString("room")

            val socketId =
                data.optString("socketId")


            Log.d(
                "LiveQueueSocket",
                "================================"
            )

            Log.d(
                "LiveQueueSocket",
                "✅ ROOM JOIN CONFIRMED"
            )

            Log.d(
                "LiveQueueSocket",
                "Queue ID = $joinedQueueId"
            )

            Log.d(
                "LiveQueueSocket",
                "Room = $room"
            )

            Log.d(
                "LiveQueueSocket",
                "Socket ID = $socketId"
            )

            Log.d(
                "LiveQueueSocket",
                "================================"
            )
        }


        // =====================================================
        // TOKEN CALLED
        // =====================================================

        SocketManager.on("TOKEN_CALLED") { data ->

            val eventQueueId =
                data.optString("queueId")

            val tokenNumber =
                data.optInt("tokenNumber")

            val status =
                data.optString("status")


            Log.d(
                "LiveQueueSocket",
                "================================"
            )

            Log.d(
                "LiveQueueSocket",
                "🔔 TOKEN_CALLED RECEIVED"
            )

            Log.d(
                "LiveQueueSocket",
                "Event Queue ID = $eventQueueId"
            )

            Log.d(
                "LiveQueueSocket",
                "My Queue ID = $queueId"
            )

            Log.d(
                "LiveQueueSocket",
                "Token = $tokenNumber"
            )

            Log.d(
                "LiveQueueSocket",
                "Status = $status"
            )


            // Only react to THIS queue
            if (eventQueueId != queueId) {

                Log.d(
                    "LiveQueueSocket",
                    "Ignoring event - different queue"
                )

                return@on
            }


            Log.d(
                "LiveQueueSocket",
                "✅ Event belongs to current queue"
            )

            Log.d(
                "LiveQueueSocket",
                "📡 Refreshing Live Queue API"
            )


            requireActivity().runOnUiThread {

                if (!isAdded) return@runOnUiThread

                viewModel.fetchLiveTicket(
                    queueId!!,
                    userId
                )
            }
        }


        // =====================================================
        // TOKEN COMPLETED
        // =====================================================

        SocketManager.on("TOKEN_COMPLETED") { data ->

            val eventQueueId =
                data.optString("queueId")

            val tokenNumber =
                data.optInt("tokenNumber")


            Log.d(
                "LiveQueueSocket",
                "================================"
            )

            Log.d(
                "LiveQueueSocket",
                "🔔 TOKEN_COMPLETED RECEIVED"
            )

            Log.d(
                "LiveQueueSocket",
                "Queue ID = $eventQueueId"
            )

            Log.d(
                "LiveQueueSocket",
                "Token = $tokenNumber"
            )


            if (eventQueueId != queueId) {

                Log.d(
                    "LiveQueueSocket",
                    "Ignoring event - different queue"
                )

                return@on
            }


            Log.d(
                "LiveQueueSocket",
                "✅ Refreshing Live Queue API"
            )


            requireActivity().runOnUiThread {

                if (!isAdded) return@runOnUiThread

                viewModel.fetchLiveTicket(
                    queueId!!,
                    userId
                )
            }
        }


        // =====================================================
        // QUEUE STATUS CHANGED
        // =====================================================

        SocketManager.on("QUEUE_STATUS_CHANGED") { data ->

            val eventQueueId =
                data.optString("queueId")

            val queueStatus =
                data.optString("queueStatus")

            val isActive =
                data.optBoolean("isActive")


            Log.d(
                "LiveQueueSocket",
                "================================"
            )

            Log.d(
                "LiveQueueSocket",
                "🔔 QUEUE_STATUS_CHANGED"
            )

            Log.d(
                "LiveQueueSocket",
                "Queue ID = $eventQueueId"

            )

            Log.d(
                "LiveQueueSocket",
                "Status = $queueStatus"
            )

            Log.d(
                "LiveQueueSocket",
                "Active = $isActive"
            )


            if (eventQueueId != queueId) {

                Log.d(
                    "LiveQueueSocket",
                    "Ignoring event - different queue"
                )

                return@on
            }


            Log.d(
                "LiveQueueSocket",
                "✅ Refreshing Live Queue API"
            )


            requireActivity().runOnUiThread {

                if (!isAdded) return@runOnUiThread

                viewModel.fetchLiveTicket(
                    queueId!!,
                    userId
                )
            }
        }


        // =====================================================
        // QUEUE SESSION ENDED
        // =====================================================

        SocketManager.on("QUEUE_SESSION_ENDED") { data ->

            val eventQueueId =
                data.optString("queueId")


            Log.d(
                "LiveQueueSocket",
                "================================"
            )

            Log.d(
                "LiveQueueSocket",
                "🔔 QUEUE_SESSION_ENDED"
            )

            Log.d(
                "LiveQueueSocket",
                "Queue ID = $eventQueueId"
            )


            if (eventQueueId != queueId) {

                Log.d(
                    "LiveQueueSocket",
                    "Ignoring event - different queue"
                )

                return@on
            }


            requireActivity().runOnUiThread {

                if (!isAdded) return@runOnUiThread

                Toast.makeText(
                    requireContext(),
                    "Queue session ended",
                    Toast.LENGTH_SHORT
                ).show()


                // Refresh one final time
                viewModel.fetchLiveTicket(
                    queueId!!,
                    userId
                )
            }
        }
    }


    // =========================================================
    // OBSERVE LIVE TICKET
    // =========================================================

    private fun observeLiveTicket() {

        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                viewModel.liveTicketState.collect { state ->

                    when (state) {

                        is Resource.Loading -> {

                            Log.d(
                                "LiveQueueSocket",
                                "Live ticket loading..."
                            )
                        }


                        is Resource.Success -> {

                            Log.d(
                                "LiveQueueSocket",
                                "✅ Live ticket API SUCCESS"
                            )

                            showTicket(state.data)
                        }


                        is Resource.Error -> {

                            Log.e(
                                "LiveQueueSocket",
                                "❌ Live ticket API ERROR: ${state.message}"
                            )

                            Toast.makeText(
                                requireContext(),
                                state.message,
                                Toast.LENGTH_SHORT
                            ).show()
                        }


                        null -> {
                            // Nothing yet
                        }
                    }
                }
            }
        }
    }


    // =========================================================
    // SHOW TICKET
    // =========================================================

    private fun showTicket(data: QueueTicketData) {

        binding.tvHospitalName.text =
            data.hospitalName

        binding.tvDoctor.text =
            data.doctorName

        binding.tvDepartment.text =
            data.department

        binding.tvUserNotes.text =
            if (data.notes.isNotBlank()) {
                data.notes
            } else {
                "No notes added"
            }



        binding.tvTokenNumber.text =
            data.tokenNumber.toString()

        binding.tvStatus.text =
            "●  ${data.status}"

        binding.tvPeopleAhead.text =
            data.peopleAheadText

        binding.tvEstimatedWait.text =
            data.estWaitTimeText

        binding.tvQueueDate.text =
            data.queueDate

        binding.tvPaymentStatus.text =
            "✓ Paid"

        updateQueueMessage(data)

        updateDoctorAvailability(data)

        updateLeaveButton(data)
    }


    // =========================================================
    // QUEUE MESSAGE
    // =========================================================

    private fun updateQueueMessage(
        data: QueueTicketData
    ) {

        when (data.status) {

            "WAITING" -> {

                val peopleAhead =
                    data.peopleAheadText
                        .filter { it.isDigit() }
                        .toIntOrNull() ?: 0



                    binding.tvQueueMessage.text =
                        "$peopleAhead people are ahead of you"

            }


            "IN_CONSULTATION" -> {

                binding.tvQueueMessage.text =
                    "Your consultation is in progress"
            }


            "COMPLETED" -> {

                binding.tvQueueMessage.text =
                    "Consultation completed"
            }


            "CANCELLED" -> {

                binding.tvQueueMessage.text =
                    "You have left this queue"
            }


            else -> {

                binding.tvQueueMessage.text =
                    "Queue status: ${data.status}"
            }
        }
    }


    // =========================================================
    // DOCTOR AVAILABILITY
    // =========================================================

    private fun updateDoctorAvailability(
        data: QueueTicketData
    ) {

        if (data.isDoctorOnDuty) {

            binding.tvDoctorAvailability.text =
                "On Duty"

        } else {

            binding.tvDoctorAvailability.text =
                "Off Duty"
        }
    }


    // =========================================================
    // LEAVE BUTTON
    // =========================================================

    private fun updateLeaveButton(
        data: QueueTicketData
    ) {

        when (data.status) {

            "WAITING" -> {

                binding.btnLeaveQueue.visibility =
                    View.VISIBLE
            }


            else -> {

                binding.btnLeaveQueue.visibility =
                    View.GONE
            }
        }
    }


    // =========================================================
    // LEAVE QUEUE CONFIRMATION
    // =========================================================

    private fun showLeaveQueueConfirmation() {

        androidx.appcompat.app.AlertDialog.Builder(
            requireContext()
        )

            .setTitle("Leave Queue?")

            .setMessage(
                "Are you sure you want to leave this queue? " +
                        "Your token will be cancelled."
            )

            .setNegativeButton(
                "Cancel",
                null
            )

            .setPositiveButton(
                "Leave Queue"
            ) { _, _ ->

                leaveQueue()
            }

            .show()
    }


    // =========================================================
    // LEAVE QUEUE
    // =========================================================

    private fun leaveQueue() {

        queueId?.let { id ->

            Log.d(
                "LiveQueueSocket",
                "Leaving queue: $id"
            )

            viewModel.leaveQueue(id)

        } ?: run {

            Toast.makeText(
                requireContext(),
                "Queue ID not found",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    // =========================================================
    // OBSERVE LEAVE QUEUE
    // =========================================================

    private fun observeLeaveQueueState() {

        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                viewModel.leaveQueueState.collect { state ->

                    when (state) {

                        is Resource.Loading -> {

                            binding.btnLeaveQueue.isEnabled =
                                false
                        }


                        is Resource.Success -> {

                            binding.btnLeaveQueue.isEnabled =
                                true

                            val successMessage =
                                state.data?.toString()
                                    ?: "Successfully left the queue"


                            Toast.makeText(
                                requireContext(),
                                successMessage,
                                Toast.LENGTH_SHORT
                            ).show()


                            parentFragmentManager.popBackStack()
                        }


                        is Resource.Error -> {

                            binding.btnLeaveQueue.isEnabled =
                                true

                            Toast.makeText(
                                requireContext(),
                                state.message,
                                Toast.LENGTH_SHORT
                            ).show()
                        }


                        null -> {
                            // Initial state
                        }
                    }
                }
            }
        }
    }

    private fun setupClickListeners() {

        binding.btnLeaveQueue.setOnClickListener {

            Log.d(
                "LiveQueueSocket",
                "Leave Queue button clicked"
            )

            showLeaveQueueConfirmation()
        }
    }
    // =========================================================
    // DESTROY VIEW
    // =========================================================

    override fun onDestroyView() {

        Log.d(
            "LiveQueueSocket",
            "LiveQueueTicket destroyed"
        )

        // Remove listeners


        // Leave this queue room
        queueId?.let { id ->

            Log.d(
                "LiveQueueSocket",
                "Leaving Socket.IO room: queue_$id"
            )

            SocketManager.leaveQueue(id)
        }


        super.onDestroyView()

        _binding = null
    }
}
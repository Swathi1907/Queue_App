package com.swathi.queue_app.v2.fragments.user

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import android.util.Log
import com.swathi.queue_app.v2.utilis.SocketManager
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.swathi.queue_app.R
import com.swathi.queue_app.databinding.UserQueuesBinding
import com.swathi.queue_app.v2.adapter.ActiveQueueAdapter
import com.swathi.queue_app.v2.adapter.QueueHistoryAdapter
import com.swathi.queue_app.v2.utilis.TokenManager
import com.swathi.queue_app.v2.viewmodels.DashboardState
import com.swathi.queue_app.v2.viewmodels.Queueviewmodel
import kotlin.apply

class QueueDashboardFragment : Fragment() {

    private var _binding: UserQueuesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: Queueviewmodel by viewModels()
    private lateinit var tokenManager: TokenManager
    private var userId: String = ""

    private lateinit var activeQueueAdapter: ActiveQueueAdapter
    private lateinit var historyAdapter: QueueHistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = UserQueuesBinding.inflate(inflater, container, false)
        tokenManager = TokenManager(requireContext())

        // Retrieve userId early so it's available for onResume()
        userId = arguments?.getString("USER_ID") ?: tokenManager.getUserId() ?: ""

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        setupObservers()
        setupListeners()
        setupSocketListeners()
    }

    override fun onResume() {
        super.onResume()
        // Now userId is reliably present when returning to this screen via backstack
        if (userId.isNotEmpty()) {
            viewModel.loadDashboardData(userId)
        } else {
            Toast.makeText(requireContext(), "User session not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerViews() {
        activeQueueAdapter = ActiveQueueAdapter(emptyList()) { queueId ->

            val bundle = Bundle().apply {
                putString("Queue_Id", queueId)
            }

            val fragment = LiveQueueTicket().apply {
                arguments = bundle
            }

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
        }

        binding.rvActiveQueues.apply {

            layoutManager = LinearLayoutManager(requireContext())
           // Toast.makeText(requireContext(),"Adapter attached",Toast.LENGTH_SHORT).show()
            adapter = activeQueueAdapter
        }

        historyAdapter = QueueHistoryAdapter(emptyList())
        binding.rvRecentHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historyAdapter
        }
    }

    private fun setupObservers() {
        viewModel.dashboardState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is DashboardState.Loading -> {
                    // Optional: Show loading indicators or shimmer layout
                }
                is DashboardState.Success -> {

                    val activeList = state.data?.activeQueue ?: emptyList()
                    val historyList = state.data?.recentHistory ?: emptyList()

                    Toast.makeText(
                        requireContext(),
                        "Active queues: ${activeList.size}",
                        Toast.LENGTH_LONG
                    ).show()

                    println("DASHBOARD ACTIVE QUEUES = $activeList")
                    println("DASHBOARD HISTORY = $historyList")

                    // Update UI
                    activeQueueAdapter.updateData(activeList)
                    historyAdapter.updateData(historyList)

                    // Join Socket.IO room for EVERY active queue
                    activeList.forEach { queue ->

                        if (queue.queueId.isNotEmpty()) {

                            SocketManager.joinQueue(queue.queueId)

                            Log.d(
                                "QueueDashboard",
                                "Joined Socket.IO room: queue_${queue.queueId}"
                            )
                        }
                    }
                }
                is DashboardState.Error -> {
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    private fun setupSocketListeners() {

        // -----------------------------------------
        // ROOM JOIN CONFIRMATION
        // -----------------------------------------

        SocketManager.on("QUEUE_ROOM_JOINED") { data ->

            val queueId = data.optString("queueId")
            val room = data.optString("room")
            val socketId = data.optString("socketId")

            Log.d("QueueSocket", "================================")
            Log.d("QueueSocket", "✅ ROOM JOIN CONFIRMED")
            Log.d("QueueSocket", "Queue ID: $queueId")
            Log.d("QueueSocket", "Room: $room")
            Log.d("QueueSocket", "Socket ID: $socketId")
            Log.d("QueueSocket", "================================")
        }


        // -----------------------------------------
        // TOKEN CALLED
        // -----------------------------------------

        SocketManager.on("TOKEN_CALLED") { data ->

            val queueId = data.optString("queueId")
            val tokenNumber = data.optInt("tokenNumber")
            val status = data.optString("status")

            Log.d(
                "QueueSocket",
                "TOKEN_CALLED queue=$queueId token=$tokenNumber status=$status"
            )

            requireActivity().runOnUiThread {

                if (!isAdded) return@runOnUiThread

                viewModel.loadDashboardData(userId)
            }
        }


        // -----------------------------------------
        // TOKEN COMPLETED
        // -----------------------------------------

        SocketManager.on("TOKEN_COMPLETED") { data ->

            val queueId = data.optString("queueId")
            val tokenNumber = data.optInt("tokenNumber")

            Log.d(
                "QueueSocket",
                "TOKEN_COMPLETED queue=$queueId token=$tokenNumber"
            )

            requireActivity().runOnUiThread {

                if (!isAdded) return@runOnUiThread

                viewModel.loadDashboardData(userId)
            }
        }


        // -----------------------------------------
        // QUEUE STATUS CHANGED
        // -----------------------------------------

        SocketManager.on("QUEUE_STATUS_CHANGED") { data ->

            val queueId = data.optString("queueId")
            val queueStatus = data.optString("queueStatus")
            val isActive = data.optBoolean("isActive")

            Log.d(
                "QueueSocket",
                "QUEUE_STATUS_CHANGED queue=$queueId status=$queueStatus active=$isActive"
            )

            requireActivity().runOnUiThread {

                if (!isAdded) return@runOnUiThread

                viewModel.loadDashboardData(userId)
            }
        }


        // -----------------------------------------
        // QUEUE SESSION ENDED
        // -----------------------------------------

        SocketManager.on("QUEUE_SESSION_ENDED") { data ->

            val queueId = data.optString("queueId")

            Log.d(
                "QueueSocket",
                "QUEUE_SESSION_ENDED queue=$queueId"
            )

            requireActivity().runOnUiThread {

                if (!isAdded) return@runOnUiThread

                viewModel.loadDashboardData(userId)

                Toast.makeText(
                    requireContext(),
                    "Queue session ended",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    private fun setupListeners() {
        binding.ivNotification.setOnClickListener {
            Toast.makeText(requireContext(), "Notifications clicked", Toast.LENGTH_SHORT).show()
        }

        binding.ivProfile.setOnClickListener {
            Toast.makeText(requireContext(), "Profile clicked", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {


        super.onDestroyView()
        _binding = null
    }
}
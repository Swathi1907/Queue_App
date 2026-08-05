package com.swathi.queue_app.v2.fragments.user

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.swathi.queue_app.databinding.UserQueuesBinding // Ensure package name matches your project structure
import com.swathi.queue_app.v2.adapter.ActiveQueueAdapter
import com.swathi.queue_app.v2.adapter.QueueHistoryAdapter
import com.swathi.queue_app.v2.viewmodels.DashboardState
import com.swathi.queue_app.v2.viewmodels.Queueviewmodel

class QueueDashboardFragment : Fragment() {

    private var _binding: UserQueuesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: Queueviewmodel by viewModels()

    private lateinit var activeQueueAdapter: ActiveQueueAdapter
    private lateinit var historyAdapter: QueueHistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = UserQueuesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        setupObservers()
        setupListeners()

        // Replace with your actual dynamic user ID retrieved from local session storage
     val  userId = arguments?.getString("USER_ID") ?: ""

        viewModel.loadDashboardData(userId)
    }

    private fun setupRecyclerViews() {
        // Initialize Active Queue Adapter
        activeQueueAdapter = ActiveQueueAdapter(emptyList()) { queueId ->
            Toast.makeText(requireContext(), "Opening QR for Queue: $queueId", Toast.LENGTH_SHORT).show()
        }
        binding.rvActiveQueues.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = activeQueueAdapter
        }

        // Initialize Recent History Adapter
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

                    activeQueueAdapter.updateData(activeList)
                    historyAdapter.updateData(historyList)
                }
                is DashboardState.Error -> {
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
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
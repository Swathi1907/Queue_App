package com.swathi.queue_app.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.swathi.queue_app.databinding.UserQueuesBinding
import com.swathi.queue_app.v2.viewmodels.DashboardState
import com.swathi.queue_app.v2.viewmodels.Queueviewmodel
class UserQueueFragment : Fragment() {

    private var _binding: UserQueuesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: Queueviewmodel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()

        // Observe dashboard state changes
        viewModel.dashboardState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is DashboardState.Loading -> {
                    // Show loading progress bar if you have one
                }
                is DashboardState.Success -> {
                    val dashboardData = state.data
                    // Bind dashboardData?.activeQueue to your active queue adapter
                    // Bind dashboardData?.recentHistory to your recent history adapter
                }
                is DashboardState.Error -> {
                    // Handle error message (e.g., Toast.makeText(...))
                }
            }
        }

        // Trigger data load (replace with actual logged-in user ID)
        viewModel.loadDashboardData("YOUR_USER_ID_HERE")
    }

    private fun setupRecyclerViews() {
        // Setup Active Queue RecyclerView
        binding.rvActiveQueues.apply {
            layoutManager = LinearLayoutManager(requireContext())
            // TODO: Set your Active Queue Adapter here
            // adapter = activeQueueAdapter
        }

        // Setup Recent History RecyclerView
        binding.rvRecentHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            // TODO: Set your Recent History Adapter here
            // adapter = recentHistoryAdapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
package com.swathi.queue_app.v2.fragments.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.swathi.queue_app.databinding.NewAdminQueuesBinding
import com.swathi.queue_app.v2.adapter.queue.GlobalQueueAdapter
import com.swathi.queue_app.v2.models.GlobalQueueItem
import com.swathi.queue_app.v2.viewmodels.AdminDashboardViewModel

class GlobalQueueMonitorFragment : Fragment() {

    private var _binding: NewAdminQueuesBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: AdminDashboardViewModel
    private lateinit var queueAdapter: GlobalQueueAdapter

    // Master list to store full data fetched from ViewModel for filtering
    private var masterQueueList = listOf<GlobalQueueItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = NewAdminQueuesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[AdminDashboardViewModel::class.java]

        setupRecyclerView()
        observeViewModel()

        // Fetch data on load
        viewModel.fetchGlobalQueues()

        binding.btnFilterQueues.setOnClickListener { view ->
            showFilterMenu(view)
        }
    }

    private fun setupRecyclerView() {
        queueAdapter = GlobalQueueAdapter(emptyList()) { queueItem ->
            Toast.makeText(requireContext(), "Selected: ${queueItem.departmentName}", Toast.LENGTH_SHORT).show()
        }

        binding.rvGlobalQueues.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = queueAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.globalQueues.observe(viewLifecycleOwner) { remoteList ->
            // Map remote data model to local UI adapter model
            masterQueueList = remoteList.map { remote ->
                GlobalQueueItem(
                    departmentId = remote.departmentId,
                    departmentName = remote.departmentName,
                    location = remote.location,
                    waitingCount = remote.waitingCount,
                    avgWaitTime = remote.avgWaitTime,
                    loadStatus = remote.loadStatus,
                    assignedDoctorsCount = remote.assignedDoctorsCount
                )
            }
            // Display full list initially
            queueAdapter.updateData(masterQueueList)
        }
        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMsg ->
            if (!errorMsg.isNullOrEmpty()) {
                Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showFilterMenu(anchorView: View) {
        val popupMenu = PopupMenu(requireContext(), anchorView)
        popupMenu.menu.add(0, 1, 0, "All Queues")
        popupMenu.menu.add(0, 2, 1, "High Load")
        popupMenu.menu.add(0, 3, 2, "Moderate")
        popupMenu.menu.add(0, 4, 3, "Normal")
//Here is what each number corresponds to based on the method signature add(groupId, itemId, order, title):
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                1 -> {
                    queueAdapter.updateData(masterQueueList)
                    true
                }
                2 -> {
                    val filtered = masterQueueList.filter { it.loadStatus.uppercase() == "HIGH_LOAD" }
                    queueAdapter.updateData(filtered)
                    true
                }
                3 -> {
                    val filtered = masterQueueList.filter { it.loadStatus.uppercase() == "MODERATE" }
                    queueAdapter.updateData(filtered)
                    true
                }
                4 -> {
                    val filtered = masterQueueList.filter { it.loadStatus.uppercase() == "NORMAL" }
                    queueAdapter.updateData(filtered)
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
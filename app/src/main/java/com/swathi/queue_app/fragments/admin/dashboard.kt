package com.swathi.queue_app.fragments.admin

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.swathi.queue_app.R
import com.swathi.queue_app.adapter.ActiveQueueAdapter
import com.swathi.queue_app.adapter.admin.MemberAdapter
import com.swathi.queue_app.databinding.AdminDashboardBinding
import com.swathi.queue_app.viewmodel.HomeViewModel
import com.swathi.queue_app.viewmodel.Queueviewmodel
import com.swathi.queue_app.viewmodel.admin.dashboardViewModel

class DashboardFragment : Fragment() {

    private var _binding: AdminDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewmodel: Queueviewmodel by viewModels()
    private val viewModel: dashboardViewModel by viewModels()
    private val viewmmodel: HomeViewModel by viewModels()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            AdminDashboardBinding.inflate(
                inflater,
                container,
                false
            )

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(
            view,
            savedInstanceState
        )
        viewmodel.createQueueResponse.observe(
            viewLifecycleOwner
        ){

            Toast.makeText(
                requireContext(),
                it.message,
                Toast.LENGTH_SHORT
            ).show()

            viewmmodel.getAllQueues()
        }
        binding.fabCreateQueue.setOnClickListener {

            showCreateQueueDialog()
        }
        binding.rvActiveQueues.layoutManager =
            LinearLayoutManager(requireContext())
viewModel.getActiveQueues()
        viewModel.activeQueues.observe(
            viewLifecycleOwner
        ) {
            println("dashboard called")
            val adapter = ActiveQueueAdapter(it) { queue ->

                val fragment = QueueDetailsFragment()

                fragment.arguments = Bundle().apply {
                    putString("queueId", queue.queueId)
                }

                parentFragmentManager.beginTransaction()
                    .replace(R.id.adminFragmentContainer, fragment)
                    .addToBackStack(null)
                    .commit()
            }

            binding.rvActiveQueues.adapter = adapter




        }

        viewModel.dashboard()

        viewModel.dashboardResponse.observe(
            viewLifecycleOwner
        ) {

            binding.tvActiveQueues.text =
                it.activeQueues.toString()

            binding.tvWaitingUsers.text =
                it.peopleWaiting.toString()

            binding.tvServedToday.text =
                it.servedToday.toString()

            binding.tvAvgWaitTime.text =
                "${it.avgWaitTime} min"
        }
    }
    private fun showCreateQueueDialog() {

        val dialogView =
            layoutInflater.inflate(
                R.layout.dialogue_create_queue,
                null
            )

        val etQueueName =
            dialogView.findViewById<EditText>(
                R.id.etQueueName
            )

        val etQueueCapacity =
            dialogView.findViewById<EditText>(
                R.id.etQueueCapacity
            )

        val spStatus =
            dialogView.findViewById<Spinner>(
                R.id.spStatus
            )

        spStatus.adapter =
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                listOf(
                    "active",
                    "paused"
                )
            )

        MaterialAlertDialogBuilder(
            requireContext()
        )
            .setTitle("Create Queue")
            .setView(dialogView)
            .setPositiveButton("Create") { _, _ ->

                val queueName =
                    etQueueName.text.toString()

                val queueCapacity =
                    etQueueCapacity.text.toString()
                        .toIntOrNull() ?: 0

                val status =
                    spStatus.selectedItem.toString()

                viewmodel.createQueue(
                    queueName,
                    queueCapacity,
                    status
                )
            }
            .setNegativeButton(
                "Cancel",
                null
            )
            .show()
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
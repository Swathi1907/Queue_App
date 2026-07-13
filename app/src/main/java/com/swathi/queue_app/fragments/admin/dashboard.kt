package com.swathi.queue_app.fragments.admin

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.app.TimePickerDialog
import java.util.Calendar
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.core.content.ContentProviderCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
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
            val prefs = requireContext().getSharedPreferences("app", Context.MODE_PRIVATE)

            val hospitalId = prefs.getString("hospitalId", "")!!
            viewmmodel.getAllQueues(hospitalId)
        }
        binding.fabCreateQueue.setOnClickListener {

            showCreateQueueDialog()
        }
        binding.rvActiveQueues.layoutManager =
            LinearLayoutManager(requireContext())

        val prefs = requireContext().getSharedPreferences("app", Context.MODE_PRIVATE)

        val hospitalId = prefs.getString("hospitalId", "")!!
        Log.d("HOSPITAL", "hospitalId = $hospitalId")
viewModel.getActiveQueues(hospitalId)
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

        viewModel.dashboard(hospitalId)

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

        val dialogView = layoutInflater.inflate(
            R.layout.dialogue_create_queue,
            null
        )

        val etQueueName =
            dialogView.findViewById<EditText>(R.id.etQueueName)

        val etDoctorName =
            dialogView.findViewById<EditText>(R.id.etDoctorName)

        val etRoomNumber =
            dialogView.findViewById<EditText>(R.id.etRoomNumber)

        val etFloor =
            dialogView.findViewById<EditText>(R.id.etFloor)

        val etStartTime =
            dialogView.findViewById<TextInputEditText>(
                R.id.etStartTime
            )

        val etEndTime =
            dialogView.findViewById<TextInputEditText>(
                R.id.etEndTime
            )

        etStartTime.setOnClickListener {
            showTimePicker(etStartTime)
        }

        etEndTime.setOnClickListener {
            showTimePicker(etEndTime)
        }
        val etQueueCapacity =
            dialogView.findViewById<EditText>(R.id.etQueueCapacity)

        val spStatus =
            dialogView.findViewById<Spinner>(R.id.spStatus)
        val prefs =
            context?.getSharedPreferences("app", Context.MODE_PRIVATE)


        spStatus.adapter =
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                listOf("active", "paused")
            )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Create Department")
            .setView(dialogView)
            .setPositiveButton("Create") { _, _ ->
                val startTime =
                    etStartTime.text.toString().trim()

                val endTime =
                    etEndTime.text.toString().trim()
                val queueName = etQueueName.text.toString().trim()
                val doctorName = etDoctorName.text.toString().trim()
                val roomNumber = etRoomNumber.text.toString().trim()
                val floor = etFloor.text.toString().trim()

                val hId =
                    prefs?.getString("hospitalId", "")!!
                val queueCapacity =
                    etQueueCapacity.text.toString()
                        .toIntOrNull() ?: 0

                val status =
                    spStatus.selectedItem.toString()

                viewmodel.createQueue(
                    queueName = queueName,
                    queueCapacity = queueCapacity,
                    queueStatus = status,
                    hospitalId= hId,
                    doctorName = doctorName,
                    roomNumber = roomNumber,
                    floor = floor,
                    startTime = startTime,
                    endTime = endTime
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    private fun showTimePicker(editText: TextInputEditText) {

        val calendar = Calendar.getInstance()

        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(
            requireContext(),
            { _, selectedHour, selectedMinute ->

                val amPm =
                    if (selectedHour >= 12) "PM" else "AM"

                var hour12 = selectedHour % 12
                if (hour12 == 0) hour12 = 12

                val time = String.format(
                    "%02d:%02d %s",
                    hour12,
                    selectedMinute,
                    amPm
                )

                editText.setText(time)

            },
            hour,
            minute,
            false
        ).show()
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
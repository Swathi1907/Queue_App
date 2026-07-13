package com.swathi.queue_app.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.swathi.queue_app.R
import com.swathi.queue_app.adapter.QueueAdapter
import com.swathi.queue_app.databinding.HospitalQueueDetailsBinding
import com.swathi.queue_app.model.QueueModel
import com.swathi.queue_app.viewmodel.HomeViewModel
import com.swathi.queue_app.viewmodel.Queueviewmodel

class HospitalQueueDetails : Fragment() {

    private var _binding: HospitalQueueDetailsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private val queueViewModel: Queueviewmodel by viewModels()

    private var activeQueueId: String? = null

    private lateinit var hospitalId: String
    private lateinit var hospitalName: String
    private lateinit var hospitalAddress: String

    private var queueList: List<QueueModel> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            HospitalQueueDetailsBinding.inflate(
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
        super.onViewCreated(view, savedInstanceState)

        hospitalId =
            arguments?.getString("hospitalId") ?: ""

        hospitalName =
            arguments?.getString("hospitalName") ?: ""

        hospitalAddress =
            arguments?.getString("hospitalAddress") ?: ""

        binding.tvHospitalName.text = hospitalName
        binding.tvAddress.text = hospitalAddress

        binding.rvQueues.layoutManager =
            LinearLayoutManager(requireContext())
        Log.d("HOSPITAL QUEUE DETAILS","CALLING")
        viewModel.getMyActiveQueue("HospitalQueueDetails")

        viewModel.getAllQueues(hospitalId)

        viewModel.activeQueueResponse.observe(viewLifecycleOwner) {

            activeQueueId =
                if (it.active) it.queueId
                else null

            setupAdapter()
        }

        viewModel.allqueueResponse.observe(viewLifecycleOwner) {

            queueList = it

            binding.tvDepartmentCount.text =
                "${it.size} Departments"

            setupAdapter()
        }

        queueViewModel.Joinqueueresponse.observe(viewLifecycleOwner) {

            Toast.makeText(
                requireContext(),
                "Token Number : ${it.tokenNumber}",
                Toast.LENGTH_SHORT
            ).show()

            openMyQueueFragment(it.queueId)
        }
    }

    private fun setupAdapter() {

        binding.rvQueues.adapter =
            QueueAdapter(

                queueList = queueList,

                onJoinclick = { queue ->

                    queueViewModel.joinQueue(queue._id)

                },

                activeQueueId = activeQueueId,

                onViewDetails = { queue ->

                    openMyQueueFragment(queue._id)

                }
            )
    }

    private fun openMyQueueFragment(
        queueId: String
    ) {

        val fragment = myqueuefragment()

        fragment.arguments = Bundle().apply {

            putString(
                "queueId",
                queueId
            )
        }

        parentFragmentManager.beginTransaction()
            .replace(
                R.id.fragmentContainer,
                fragment
            )
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
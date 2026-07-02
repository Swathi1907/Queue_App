package com.swathi.queue_app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.swathi.queue_app.R
import com.swathi.queue_app.adapter.LiveStatusAdapter
import com.swathi.queue_app.adapter.QueueAdapter
import com.swathi.queue_app.databinding.FragmentHomeBinding
import com.swathi.queue_app.viewmodel.HomeViewModel
import com.swathi.queue_app.viewmodel.Queueviewmodel
import com.swathi.queue_app.model.QueueModel
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private val queueViewModel: Queueviewmodel by viewModels()

    private var queuesList: List<QueueModel> = emptyList()
    private var activeQueueId: String? = null
    override fun onResume() {
        super.onResume()

        viewModel.getAllQueues()
        viewModel.getMyActiveQueue()
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentHomeBinding.inflate(
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

        binding.rvLiveStatus.layoutManager =
            LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
            )

        binding.imgNotification.setOnClickListener {

            NotificationBottomSheet()
                .show(parentFragmentManager, "NotificationBottomSheet")
            viewModel.markNotificationsRead()
        }
        viewModel.readNotificationResponse.observe(viewLifecycleOwner) {

            viewModel.getNotificationCount()
        }
        viewModel.notificationCount.observe(viewLifecycleOwner) {

            if (it.count == 0) {
                binding.tvNotificationBadge.visibility = View.GONE
            } else {
                binding.tvNotificationBadge.visibility = View.VISIBLE
                binding.tvNotificationBadge.text =
                    if (it.count > 99) "99+" else it.count.toString()
            }
        }

        binding.rvQueues.layoutManager =
            LinearLayoutManager(requireContext())

        viewModel.getAllQueues()
        viewModel.getMyActiveQueue()
        viewModel.getNotificationCount()
        viewModel.allqueueResponse.observe(viewLifecycleOwner) {

            queuesList = it



            setupQueueAdapter()
        }

      //  viewModel.activeQueueResponse.observe(viewLifecycleOwner) {

        //    activeQueueId =
          //      if (it.active)
            //        it.queueId
              //  else
                //    null

            //setupQueueAdapter()
        //}
        if (activeQueueId != null) {

            binding.rvLiveStatus.visibility = View.VISIBLE
            binding.tvLiveStatus.visibility = View.VISIBLE

            queueViewModel.myStatus(activeQueueId!!)

        } else {
binding.tvLiveStatus.visibility=View.GONE
            binding.rvLiveStatus.visibility = View.GONE
            binding.tvLiveStatus.visibility = View.GONE
        }
        queueViewModel.myStatusResponse.observe(viewLifecycleOwner) {

            binding.rvLiveStatus.visibility = View.VISIBLE

            binding.rvLiveStatus.adapter =
                LiveStatusAdapter(listOf(it))
        }
        viewModel.activeQueueResponse.observe(viewLifecycleOwner) {

            activeQueueId =
                if (it.active) it.queueId else null

            if (activeQueueId != null) {
                queueViewModel.myStatus(activeQueueId!!)
            } else {
                binding.rvLiveStatus.visibility = View.GONE
            }

            setupQueueAdapter()
        }

        queueViewModel.Joinqueueresponse.observe(viewLifecycleOwner) {

            Toast.makeText(
                requireContext(),
                "Token Number : ${it.tokenNumber}",
                Toast.LENGTH_LONG
            ).show()

            activeQueueId = it.queueId

            queueViewModel.myStatus(it.queueId)

            viewModel.getAllQueues()
            viewModel.getMyActiveQueue()
        }
    }

    private fun setupQueueAdapter() {

        binding.rvQueues.adapter =
            QueueAdapter(
                queueList = queuesList,

                onJoinclick = { queue ->

                    queueViewModel.joinQueue(
                        queue._id
                    )
                },

                activeQueueId = activeQueueId,

                onViewDetails = { queue ->

                    openMyQueueFragment(
                        queue._id
                    )
                }
            )
    }

    private fun openMyQueueFragment(
        queueId: String
    ) {

        val fragment =
            myqueuefragment()

        val bundle = Bundle()

        bundle.putString(
            "queueId",
            queueId
        )

        fragment.arguments = bundle

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
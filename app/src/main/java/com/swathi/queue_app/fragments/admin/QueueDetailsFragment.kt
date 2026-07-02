package com.swathi.queue_app.fragments.admin

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
import com.swathi.queue_app.adapter.admin.MemberAdapter
import com.swathi.queue_app.databinding.AdminQueueDetailsBinding
import com.swathi.queue_app.model.QueueItemMapper
import com.swathi.queue_app.viewmodel.Queueviewmodel
import kotlin.getValue

class QueueDetailsFragment : Fragment() {

    private var _binding: AdminQueueDetailsBinding? = null
    private val binding get() = _binding!!

    private var expanded = false
    private val viewModel: Queueviewmodel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            AdminQueueDetailsBinding.inflate(
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

        val queueId =
            arguments?.getString("queueId")
        queueId?.let {
            viewModel.getQueueDetails(it)
        }
        viewModel.queueDetailsResponse.observe(
            viewLifecycleOwner
        ){ details ->

            binding.tvQueueName.text =
                details.queueName

            binding.tvCurrentToken.text =
                details.currentToken.toString()

            binding.tvWaitingUsers.text =
                details.waitingUsers.toString()

            binding.tvStatus.text =
                details.queueStatus

            binding.btnPauseQueue.text =
                if(details.queueStatus == "paused")
                    "Resume"
                else
                    "Pause"
        }
        Toast.makeText(
            requireContext(),
            "QueueId = $queueId",
            Toast.LENGTH_SHORT
        ).show()
        binding.tvViewMembers.layoutManager =
            LinearLayoutManager(requireContext())
        viewModel.allMembersResponse.observe(viewLifecycleOwner) { members ->
            val items = QueueItemMapper.map(members)
            binding.tvViewMembers.adapter = MemberAdapter(items)
        }
        binding.tvView.setOnClickListener {

            expanded = !expanded

            binding.tvView.setOnClickListener {

                expanded = !expanded

                if (expanded) {

                    binding.cardMember.visibility = View.VISIBLE
                    binding.tvView.text = "Hide Members ▲"

                    queueId?.let {
                        viewModel.getAllMembers(it)
                    }

                } else {

                    binding.cardMember.visibility = View.GONE
                    binding.tvView.text = "View Members ▼"
                }
            }
        }

        binding.cardNext.setOnClickListener {

            queueId?.let {

                viewModel.nextToken(it)
            }
        }
        viewModel.nextTokenResponse.observe(
            viewLifecycleOwner
        ){

            Toast.makeText(
                requireContext(),
                it.message,
                Toast.LENGTH_SHORT
            ).show()
        }
        binding.cardComplete.setOnClickListener {

            queueId?.let {

                viewModel.completeCurrent(it)
            }
        }

        viewModel.completeCurrentResponse.observe(
            viewLifecycleOwner
        ) {

            Toast.makeText(
                requireContext(),
                it.message,
                Toast.LENGTH_SHORT
            ).show()
        }
        binding.cardPause.setOnClickListener {

            queueId?.let {

                viewModel.toggleQueueStatus(it)
            }
        }
        viewModel.queueStatusResponse.observe(
            viewLifecycleOwner
        ) {

            Toast.makeText(
                requireContext(),
                it.message,
                Toast.LENGTH_SHORT
            ).show()

            binding.btnPauseQueue.text =
                if(it.queueStatus == "paused")
                    "Resume"
                else
                    "Pause"
            binding.tvStatus.text=it.queueStatus
        }
        binding.cardClose.setOnClickListener {

            queueId?.let {

                viewModel.closeQueue(it)
            }
        }
        viewModel.closeQueueResponse.observe(
            viewLifecycleOwner
        ){

            Toast.makeText(
                requireContext(),
                it.message,
                Toast.LENGTH_SHORT
            ).show()
            requireActivity()
                .supportFragmentManager
                .beginTransaction()
                .replace(
                    R.id.adminFragmentContainer,
                    DashboardFragment()
                )
                .commit()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
package com.swathi.queue_app.fragments
import android.graphics.Paint
import androidx.fragment.app.Fragment
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.swathi.queue_app.R
import com.swathi.queue_app.SocketManager
import com.swathi.queue_app.adapter.admin.MemberAdapter
import com.swathi.queue_app.viewmodel.Queueviewmodel
import com.swathi.queue_app.databinding.MyqueuefragmentBinding
import com.swathi.queue_app.model.QueueItemMapper
import com.swathi.queue_app.viewmodel.HomeViewModel

class myqueuefragment : Fragment() {

    private var _binding: MyqueuefragmentBinding? = null
    private val binding get() = _binding!!
    private var myUserId = ""
    private val homeViewModel: HomeViewModel by viewModels()
    private val viewModel: Queueviewmodel by viewModels()
    private lateinit var queueListener: io.socket.emitter.Emitter.Listener

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = MyqueuefragmentBinding.inflate(
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
        var queueId = arguments?.getString("queueId")
println(queueId)
        if (queueId != null) {
            observeQueueUpdates(queueId)
            viewModel.myStatus(queueId)
            viewModel.getUserMembers(queueId)
        } else {
            Log.d("TRACE","FROM MY QUEUFRAGMENT")
            homeViewModel.getMyActiveQueue()
            homeViewModel.activeQueueResponse.observe(viewLifecycleOwner) {
                if (it.active) {
                    val activeQueueId = it.queueId
                    observeQueueUpdates(activeQueueId)
                    viewModel.myStatus(activeQueueId)
                    Log.d("QUEUE_ID", "Fetching members for: $it.queueId")

                    viewModel.getUserMembers(activeQueueId)
                }
            }
        }
        binding.btnLeaveQueue.setOnClickListener {
println("Exit button clicked");

            queueId?.let {

                viewModel.exitQueue(it)
            }


            println("$queueId is null");
        }

        viewModel.exitQueueResponse.observe(viewLifecycleOwner) {

            if (::queueListener.isInitialized) {
                SocketManager.getSocket().off("queueUpdated", queueListener)
            }

            Toast.makeText(
                requireContext(),
                "Exited queue successfully",
                Toast.LENGTH_SHORT
            ).show()

            parentFragmentManager.popBackStack()
        }
        viewModel.queueNotFound.observe(viewLifecycleOwner) { notFound ->

            if (notFound) {

                viewModel.resetQueueNotFound()

                Toast.makeText(
                    requireContext(),
                    "You have been served",
                    Toast.LENGTH_SHORT
                ).show()

                parentFragmentManager.popBackStack()
            }
        }
        viewModel.myStatusResponse.observe(viewLifecycleOwner) {
            println("RESPONSE = $it")
            when (it.queue_status) {

                "WAITING_TO_START" -> {
                    binding.tvQueueProgressStatus.text =
                        "⏳ Queue hasn't started yet"
                }

                "WAITING_FOR_NEXT_CALL" -> {
                    binding.tvQueueProgressStatus.text =
                        "🟡 You're next. Waiting for the receptionist to call you."
                }

                "WAITING" -> {
                    binding.tvQueueProgressStatus.text =
                        "👥 ${it.peopleAhead} people ahead of you"
                }

                "NEXT" -> {
                    binding.tvQueueProgressStatus.text =
                        "🔔 You're next. Please be ready."
                }

                "SERVING" -> {
                    binding.tvQueueProgressStatus.text =
                        "🟢 It's your turn. Please proceed to the doctor."
                }

                else -> {
                    binding.tvQueueProgressStatus.text = ""
                }
            }

            myUserId = it.userId
            binding.tvQueueName.text =
            it.queueName
binding.tvPeopleAhead.text="${it.peopleAhead}"

            binding.btnLeaveQueue.paintFlags =
                binding.btnLeaveQueue.paintFlags or Paint.UNDERLINE_TEXT_FLAG
            binding.tvYourToken.text =
                it.yourToken.toString()

println(it.QueueStatus)
           // if (it.QueueStatus == "active") {
             //   binding.tvStatus.text = "🟢 Active"
               // binding.tvStatus.setTextColor(
                 //   ContextCompat.getColor(requireContext(), R.color.green)
                //)
           // } else if (it.QueueStatus == "paused") {
             //   binding.tvStatus.text = "🟡 Paused"
               // binding.tvStatus.setTextColor(
                 //   ContextCompat.getColor(requireContext(), R.color.yellow)
                //)
            //}

            if (it.status == "serving") {

                binding.btnLeaveQueue.isEnabled = false
                binding.btnLeaveQueue.alpha = 0.5f
                binding.btnLeaveQueue.text = "Leave Queue"


            } else {
                binding.btnLeaveQueue.isEnabled = true
                binding.btnLeaveQueue.alpha = 1f
                binding.btnLeaveQueue.text = "Leave Queue"
            }
            if (it.currentToken == null) {
                binding.tvCurrentToken.text = "-"
            } else {

                binding.tvCurrentToken.text = it.currentToken.toString()


            }

            if (it.peopleAhead <= 0) {
                binding.tvWaitTime.text = "${it.avgServiceTime}"
            } else {
                val waitTime = it.peopleAhead * it.avgServiceTime
                binding.tvWaitTime.text = "$waitTime m"
            }


        }

        binding.rvMembers.layoutManager =
            LinearLayoutManager(requireContext())



        viewModel.userMembers.observe(viewLifecycleOwner) { members ->
            val items = QueueItemMapper.map(members)
            Log.d("USER_MEMBERS", "Observer size = ${members.size}")
            val adapter = MemberAdapter(items)
            binding.rvMembers.adapter = adapter
        }
    }
    private fun observeQueueUpdates(queueId: String) {

        queueListener = io.socket.emitter.Emitter.Listener {

            requireActivity().runOnUiThread {

                Log.d("SOCKET", "Queue Updated Received")

                viewModel.myStatus(queueId)
                viewModel.getUserMembers(queueId)
            }
        }

        SocketManager.getSocket().on("queueUpdated", queueListener)
    }
    override fun onDestroyView() {

        if (::queueListener.isInitialized) {
            SocketManager.getSocket().off("queueUpdated", queueListener)
        }

        super.onDestroyView()
        _binding = null
    }
}
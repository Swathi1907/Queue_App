package com.swathi.queue_app.fragments

import android.content.Context
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
import com.swathi.queue_app.SocketManager
import com.swathi.queue_app.adapter.HospitalAdapter
import com.swathi.queue_app.adapter.LiveStatusAdapter
import com.swathi.queue_app.adapter.QueueAdapter
import com.swathi.queue_app.databinding.FragmentHomeBinding
import com.swathi.queue_app.model.HospitalModel
import com.swathi.queue_app.viewmodel.HomeViewModel
import com.swathi.queue_app.viewmodel.Queueviewmodel
import com.swathi.queue_app.model.QueueModel
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private val queueViewModel: Queueviewmodel by viewModels()

    private var activeQueueId: String? = null
    override fun onResume() {
        super.onResume()

        viewModel.getAllHospitals()
        Log.d("TRACE", "Called from onResume")
        viewModel.getMyActiveQueue("HomeFragment onResume")
        Log.d("HOME", "onResume called")
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

        binding.rvhospitals.layoutManager =
            LinearLayoutManager(requireContext())
        Log.d("TRACE", "Called from onViewCreated")
        viewModel.getMyActiveQueue("HomeFragment onviewcreated")
        viewModel.getNotificationCount()
        viewModel.hospitalResponse.observe(viewLifecycleOwner) { hospitals ->

            Log.d("HOME", "Hospitals received = ${hospitals.size}")
            Log.d("HOME", hospitals.toString())

            setupHospitalAdapter(hospitals)
        }
        SocketManager.getSocket().off("queueUpdated")
        SocketManager.getSocket().on("queueUpdated") {

            requireActivity().runOnUiThread {

                Log.d("SOCKET", "Queue Updated Received")

                viewModel.getAllHospitals()

                // Only refresh active queue.
                // activeQueueResponse observer will decide whether to call myStatus().
                Log.d("TRACE", "Called from Socket")

                viewModel.getMyActiveQueue("HomeFragment socket")
            }
        }
      //  viewModel.activeQueueResponse.observe(viewLifecycleOwner) {

        //    activeQueueId =
          //      if (it.active)
            //        it.queueId
              //  else
                //    null

            //setupQueueAdapter()package com.swathi.queue_app.model
        //
        //data class CreateQueueRequest(
        //    val queueName: String,
        //    val queueCapacity: Int,
        //    val queueStatus: String,
        //    val doctorName: String,
        //   val  roomNumber:String,
        //  val   floor:String,
        //   val  startTime: String,
        //   val  endTime:String
        //
        //}
        viewModel.activeQueueResponse.observe(viewLifecycleOwner) { queues ->

            if (queues.isEmpty()) {

                binding.rvLiveStatus.visibility = View.GONE
                binding.tvLiveStatus.visibility = View.GONE

            } else {

                binding.rvLiveStatus.visibility = View.VISIBLE
                binding.tvLiveStatus.visibility = View.VISIBLE
                binding.rvLiveStatus.adapter = LiveStatusAdapter(queues) { queue ->

                    val bundle = Bundle().apply {
                        putString("queueId", queue.queueId)
                    }
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, myqueuefragment())
                        .addToBackStack(null)
                        .commit()
                }
            }
        }

      /*  queueViewModel.myStatusResponse.observe(viewLifecycleOwner) {

            binding.rvLiveStatus.visibility = View.VISIBLE

            binding.rvLiveStatus.adapter =
                LiveStatusAdapter(listOf(it))
        } */
       /* viewModel.activeQueueResponse.observe(viewLifecycleOwner) {

            Log.d(
                "HOME",
                "active=${it.active}, queueId=${it.queueId}"
            )

            activeQueueId = if (it.active) it.queueId else null

            if (it.active) {
                binding.rvLiveStatus.visibility = View.VISIBLE
                binding.tvLiveStatus.visibility = View.VISIBLE
                queueViewModel.myStatus(it.queueId)
            } else {
                binding.rvLiveStatus.visibility = View.GONE
                binding.tvLiveStatus.visibility = View.GONE
                activeQueueId = null
            }
            }
*/

        viewModel.activeQueueResponse.observe(viewLifecycleOwner) { queues ->

            val activeQueue = queues.firstOrNull()

            if (activeQueue != null) {

                Log.d(
                    "HOME",
                    "queueId=${activeQueue.queueId}"
                )

                activeQueueId = activeQueue.queueId

                binding.rvLiveStatus.visibility = View.VISIBLE
                binding.tvLiveStatus.visibility = View.VISIBLE

                queueViewModel.myStatus(activeQueue.queueId)

            } else {

                activeQueueId = null

                binding.rvLiveStatus.visibility = View.GONE
                binding.tvLiveStatus.visibility = View.GONE
            }
        }
      /*  viewModel.activeQueueResponse.observe(viewLifecycleOwner) {

            Log.d("HOME", "active=${it.active}, queueId=${it.queueId}")

            activeQueueId = if (it.active) it.queueId else null

            if (it.active) {
                binding.rvLiveStatus.visibility = View.VISIBLE
                binding.tvLiveStatus.visibility = View.VISIBLE

                queueViewModel.myStatus(it.queueId)

            } else {

                activeQueueId = null
                binding.rvLiveStatus.visibility = View.GONE
                binding.tvLiveStatus.visibility = View.GONE
            }
        } */



    }

    private fun setupHospitalAdapter(
        hospitals: List<HospitalModel>
    ) {

        binding.rvhospitals.adapter =
            HospitalAdapter(hospitals) { hospital ->

                openHospitalQueuesFragment(
                    hospital
                )
            }
    }
    private fun openHospitalQueuesFragment(
        hospital: HospitalModel
    ) {

        val fragment = HospitalQueueDetails()

        fragment.arguments = Bundle().apply {
            putString("hospitalId", hospital.hospitalId)
            putString("hospitalName", hospital.hospitalName)
            putString(
                "hospitalAddress",
                "${hospital.address}, ${hospital.city}"
            )
        }

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
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
        SocketManager.getSocket().off("queueUpdated")
        super.onDestroyView()
        _binding = null

    }
}
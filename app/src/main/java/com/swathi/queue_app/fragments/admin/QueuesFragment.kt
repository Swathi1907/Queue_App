package com.swathi.queue_app.fragments.admin

import android.content.Context
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
import com.swathi.queue_app.adapter.admin.AdminQueueAdapter

import com.swathi.queue_app.databinding.AdminQueuesBinding
import com.swathi.queue_app.viewmodel.HomeViewModel
import com.swathi.queue_app.viewmodel.Queueviewmodel
class QueuesFragment : Fragment() {

    private var _binding: AdminQueuesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    private val viewmodel: Queueviewmodel by viewModels()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            AdminQueuesBinding.inflate(
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



    //    viewmodel.createQueueResponse.observe(
      //      viewLifecycleOwner
       // ){

         //   Toast.makeText(
           //     requireContext(),
             //   it.message,
               // Toast.LENGTH_SHORT
         //   ).show()

           // viewModel.getAllQueues()
        //}
        //binding.fabCreateQueue.setOnClickListener {

          //  showCreateQueueDialog()
       // }





        binding.rvadminqueues.layoutManager=
            LinearLayoutManager(
                requireContext()
            )

        val prefs = requireContext().getSharedPreferences("app", Context.MODE_PRIVATE)

        val hospitalId = prefs.getString("hospitalId", "")!!

        viewModel.getAllQueues(hospitalId)

        viewModel.allqueueResponse.observe(
            viewLifecycleOwner
        ) { queues ->

            binding.rvadminqueues.adapter=
                AdminQueueAdapter(
                    queues
                ) { queue ->

                   openQueueDetailsFragment(
                       queue._id
                   )
                    // Later:
                    // openQueueDetailsFragment(queue)

                }

        }
    }
    private fun openQueueDetailsFragment(
        queueId: String
    ) {

        val fragment: Fragment =
            QueueDetailsFragment()

        val bundle = Bundle()

        bundle.putString(
            "queueId",
            queueId
        )

        fragment.arguments = bundle

        parentFragmentManager.beginTransaction()
            .replace(
                R.id.adminFragmentContainer,
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
package com.swathi.queue_app.v2.fragments.user

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.swathi.queue_app.R
import com.swathi.queue_app.v2.adapter.UserDoctorAdapter
import com.swathi.queue_app.v2.viewmodels.HospitalViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DepartmentDoctorFragment : Fragment(R.layout.department_doctor_fragment) {

    private lateinit var viewModel: HospitalViewModel
    private lateinit var tvPageTitle: TextView
    private lateinit var chipDepartment: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var doctorAdapter: UserDoctorAdapter

    private var hospitalId: String = ""
    private var departmentName: String = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Retrieve arguments passed from UserHospitalFragment
        hospitalId = arguments?.getString("hospitalId") ?: ""
        departmentName = arguments?.getString("departmentName") ?: ""

        // Bind Views matching your XML layout IDs
        tvPageTitle = view.findViewById(R.id.tvPageTitle)
        chipDepartment = view.findViewById(R.id.chipDepartment)
        recyclerView = view.findViewById(R.id.recyclerDoctors)
        progressBar = view.findViewById(R.id.progressBarDoctors)

        // Set dynamic text based on the selected department
        tvPageTitle.text = "$departmentName Specialists"
        chipDepartment.text = departmentName

        // Setup RecyclerView with UserDoctorAdapter correctly
        doctorAdapter = UserDoctorAdapter(emptyList()) { selectedDoctor ->
            // Package the selected doctor's real data into a Bundle
            val bundle = Bundle().apply {
                putString("DOCTOR_CODE", selectedDoctor.doctorCode)
                putInt("CONSULTATION_FEE_INR", selectedDoctor.consultationFee.toInt())
                putString("DOCTOR_NAME", selectedDoctor.name)
                putString("SPECIALTY", selectedDoctor.specialty ?: departmentName)
                putString("WAIT_TIME", "🕒 Current Wait: ~${selectedDoctor.estimatedWaitTime} mins")
            }

            // Instantiate JoinQueueFragment and attach arguments
            val joinQueueFragment = JoinQueueFragment().apply {
                arguments = bundle
            }

            // Perform manual fragment transaction to open JoinQueueFragment
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, joinQueueFragment) // Match your activity container ID
                .addToBackStack(null)
                .commit()
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = doctorAdapter

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[HospitalViewModel::class.java]

        // Observe user doctors state flow
        observeUserDoctorsState()

        // Fetch user-side doctors for this department
        if (hospitalId.isNotEmpty() && departmentName.isNotEmpty()) {
            viewModel.fetchUserDoctors(hospitalId, departmentName)
        }
    }

    private fun observeUserDoctorsState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.userDoctorsState.collectLatest { resource ->
                when (resource) {
                    is HospitalViewModel.Resource.Loading -> {
                        progressBar.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                    }
                    is HospitalViewModel.Resource.Success -> {
                        progressBar.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                        doctorAdapter.updateData(resource.data)
                    }
                    is HospitalViewModel.Resource.Error -> {
                        progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show()
                    }
                    is HospitalViewModel.Resource.Idle -> {
                        progressBar.visibility = View.GONE
                    }
                }
            }
        }
    }
}
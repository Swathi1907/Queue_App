package com.swathi.queue_app.v2.fragments.user

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.swathi.queue_app.R
import com.swathi.queue_app.v2.adapter.DepartmentAdapter
import com.swathi.queue_app.v2.adapter.UserDepartmentAdapter
import com.swathi.queue_app.v2.fragments.SelectDoctorFragment
import com.swathi.queue_app.v2.utilis.TokenManager
import com.swathi.queue_app.v2.viewmodels.HospitalViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class UserHospitalFragment : Fragment(R.layout.user_hospital_page) {

    private lateinit var viewModel: HospitalViewModel
    private lateinit var toolbar: Toolbar
    private lateinit var tvHospitalName: TextView
    private lateinit var tvHospitalAddress: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var departmentAdapter: UserDepartmentAdapter

    private var hospitalId: String = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Retrieve hospitalId from arguments
        hospitalId = arguments?.getString("HOSPITAL_CODE") ?: ""
Log.d("userhospfrag","${hospitalId} received")
        // Bind Views
        toolbar = view.findViewById(R.id.toolbar)
        tvHospitalName = view.findViewById(R.id.tvHospitalName)
        tvHospitalAddress = view.findViewById(R.id.tvAddress)
        recyclerView = view.findViewById(R.id.recyclerDepartments)
        progressBar = view.findViewById(R.id.progressBarDepartments)

        // Setup toolbar back navigation
        toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // Setup RecyclerView Grid
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        departmentAdapter = UserDepartmentAdapter(emptyList()) { selectedDepartmentItem ->

            val bundle = Bundle().apply {
                putString("HOSPITAL_CODE", hospitalId)
                putString("DEPARTMENT_NAME", selectedDepartmentItem.name)

            }
Log.d("userhospfrag","sending ${hospitalId}")
            val doctorFragment = DepartmentDoctorFragment().apply {
                arguments = bundle
            }

            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, doctorFragment)
                .addToBackStack(null)
                .commit()
        }
        recyclerView.adapter = departmentAdapter

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[HospitalViewModel::class.java]

        // Observe LiveData and StateFlow
        observeViewModel()

        // Load data if ID is valid
        if (hospitalId.isNotEmpty()) {
            viewModel.loadHospitalById(hospitalId)
            viewModel.fetchDepartments(hospitalId)
        }
    }

    private fun observeViewModel() {
        // Observe Hospital Details LiveData
        viewModel.hospitalDetail.observe(viewLifecycleOwner) { response ->
            response?.data?.let { hospital ->
                toolbar.title = hospital.name
                tvHospitalName.text = hospital.name
                tvHospitalAddress.text = hospital.address
            }
        }

        // Collect Department StateFlow with lifecycle scope
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.departmentState.collectLatest { resource ->
                when (resource) {
                    is HospitalViewModel.Resource.Loading -> {
                        progressBar.visibility = View.VISIBLE
                    }
                    is HospitalViewModel.Resource.Success -> {
                        progressBar.visibility = View.GONE
                        // Assuming resource.data has a list of departments (e.g., resource.data.departments)
                        // Adjust according to your DepartmentData model property name holding the list
                        departmentAdapter.updateData(resource.data.departments)
                    }
                    is HospitalViewModel.Resource.Error -> {
                        progressBar.visibility = View.GONE
                        // Handle error message state if needed
                    }
                    is HospitalViewModel.Resource.Idle -> {
                        progressBar.visibility = View.GONE
                    }
                    null -> {
                        progressBar.visibility = View.GONE
                    }
                }
            }
        }

        // Observe general error LiveData
        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            progressBar.visibility = View.GONE
        }
    }
}
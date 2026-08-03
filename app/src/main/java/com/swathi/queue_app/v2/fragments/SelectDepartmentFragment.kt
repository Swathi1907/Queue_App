package com.swathi.queue_app.v2.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.swathi.queue_app.R
import com.swathi.queue_app.v2.adapter.DepartmentAdapter
import com.swathi.queue_app.v2.viewmodels.HospitalViewModel
import kotlinx.coroutines.launch
import com.swathi.queue_app.v2.utilis.TokenManager
class SelectDepartmentFragment : Fragment(R.layout.fragment_select_department) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: DepartmentAdapter
    private lateinit var tokenManager: TokenManager

    private val viewModel: HospitalViewModel by viewModels()
    private var hospitalId: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tokenManager = TokenManager(requireContext())
        hospitalId = tokenManager.getHospitalId()

        recyclerView = view.findViewById(R.id.recyclerDepartments)
        progressBar = view.findViewById(R.id.progressBarDepartments)

        // Changed to GridLayoutManager with 2 columns to match your grid design layout
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        adapter = DepartmentAdapter(emptyList()) { selectedDepartmentItem ->
            val bundle = Bundle().apply {
                putString("departmentName", selectedDepartmentItem.name)
            }
            val doctorFragment = SelectDoctorFragment().apply {
                arguments = bundle
            }
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, doctorFragment)
                .addToBackStack(null)
                .commit()
        }
        recyclerView.adapter = adapter
        observeViewModel()
        if (!hospitalId.isNullOrEmpty()) {
            viewModel.fetchDepartments(hospitalId!!)
        } else {
            Toast.makeText(requireContext(), "Hospital ID not found. Please log in again.", Toast.LENGTH_SHORT).show()
        }
    }
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.departmentState.collect { resource ->
                    when (resource) {
                        is HospitalViewModel.Resource.Idle -> {
                            progressBar.visibility = View.GONE
                        }
                        is HospitalViewModel.Resource.Loading -> {
                            progressBar.visibility = View.VISIBLE
                        }
                        is HospitalViewModel.Resource.Success -> {
                            progressBar.visibility = View.GONE
                            resource.data?.departments?.let { departmentList ->
                                // departmentList is now List<DepartmentItem>, matching the updated adapter
                                adapter.updateData(newDepartments = departmentList)
                            }
                        }
                        is HospitalViewModel.Resource.Error -> {
                            progressBar.visibility = View.GONE
                            Log.d("selectdepart","${resource.message}")
                            Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show()
                        }
                        null -> {
                            // Handle the initial null state
                        }
                    }
                }
            }
        }
    }
}
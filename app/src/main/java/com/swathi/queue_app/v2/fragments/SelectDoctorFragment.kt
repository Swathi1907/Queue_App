package com.swathi.queue_app.v2.fragments

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.swathi.queue_app.R
import com.swathi.queue_app.v2.adapter.DoctorAdapter


import com.swathi.queue_app.v2.utilis.TokenManager
import com.swathi.queue_app.v2.viewmodels.HospitalViewModel
import kotlinx.coroutines.launch

class SelectDoctorFragment : Fragment(R.layout.fragment_doctor_list) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: DoctorAdapter
    private lateinit var tokenManager: TokenManager

    private val viewModel: HospitalViewModel by viewModels()
    private var hospitalId: String? = null
    private var departmentName: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tokenManager = TokenManager(requireContext())
        hospitalId = tokenManager.getHospitalId()
        departmentName = arguments?.getString("departmentName")

        recyclerView = view.findViewById(R.id.recyclerViewDoctors)
        progressBar = view.findViewById(R.id.progressBarDoctors)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = DoctorAdapter(emptyList()) { selectedDoctor ->
            // Pass the selected doctor details forward to verification or next flow

            // Example: navigate to Doctor ID Verification fragment
            val fragment = VerifyDoctorCodeFragment().apply {
                arguments = Bundle().apply {
                    putString("doctorId", selectedDoctor.doctorCode)
                }
            }
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
        }
        recyclerView.adapter = adapter

        observeViewModel()

        if (!hospitalId.isNullOrEmpty() && !departmentName.isNullOrEmpty()) {
            viewModel.fetchDoctors(hospitalId!!, departmentName!!)
        } else {
            Toast.makeText(requireContext(), "Missing hospital or department info.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.doctorState.collect { resource ->
                    when (resource) {
                        is HospitalViewModel.Resource.Idle -> {
                            progressBar.visibility = View.GONE
                        }
                        is HospitalViewModel.Resource.Loading -> {
                            progressBar.visibility = View.VISIBLE
                        }
                        is HospitalViewModel.Resource.Success -> {
                            progressBar.visibility = View.GONE
                            adapter.updateData(resource.data)
                        }
                        is HospitalViewModel.Resource.Error -> {
                            progressBar.visibility = View.GONE
                            Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }
}
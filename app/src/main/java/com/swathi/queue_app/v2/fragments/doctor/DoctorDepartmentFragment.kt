package com.swathi.queue_app.v2.fragments.doctor


import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.navigation.fragment.findNavController
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.swathi.queue_app.R
import com.swathi.queue_app.databinding.DoctorDepartmentBinding
import com.swathi.queue_app.v2.Activities.DoctorMainActivity
import com.swathi.queue_app.v2.adapter.doctor.DoctorDepartmentAdapter
import com.swathi.queue_app.v2.fragments.doctor.DoctorHomeFragment
import com.swathi.queue_app.v2.viewmodels.DoctorViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DoctorDepartmentFragment : Fragment() {

    private var _binding: DoctorDepartmentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DoctorViewModel by viewModels()
    private lateinit var departmentAdapter: DoctorDepartmentAdapter
    private val tokenManager by lazy { com.swathi.queue_app.v2.utilis.TokenManager(requireContext()) }
    // Hold a reference to the doctor code once fetched successfully
    private var currentDoctorCode: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DoctorDepartmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()

        // Trigger fetching profile data when screen loads
        viewModel.fetchDoctorProfile()

    }



    private fun setupRecyclerView() {
        departmentAdapter = DoctorDepartmentAdapter(emptyList()) { selectedDepartment ->
            Toast.makeText(requireContext(), "Selected: $selectedDepartment", Toast.LENGTH_SHORT).show()
            val doctorCode = tokenManager.getDoctorCode() ?: currentDoctorCode ?: ""
            Log.d("dhf", "sending department: $selectedDepartment, doctorCode: $doctorCode")

            // Pack your arguments into a Bundle
            val bundle = Bundle().apply {
                putString("DEPARTMENT", selectedDepartment)
                putString("DOCTOR_CODE", doctorCode)
            }

            // Use the Jetpack NavController to navigate.
            // This triggers the destination change listener in DoctorMainActivity automatically!
            findNavController().navigate(R.id.doctorHomeFragment, bundle)
        }

        binding.rvDepartments.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = departmentAdapter
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.doctorProfileState.collectLatest { state ->
                    when (state) {
                        is DoctorViewModel.Resource.Loading -> {
                            // Optional: Show loading indicator
                        }
                        is DoctorViewModel.Resource.Success -> {
                            // Save the doctorCode and departments from the profile response
                            currentDoctorCode = state.data.data?.doctorCode
                            val departments = state.data.data?.department ?: emptyList()

                            // Save departments to TokenManager as a comma-separated string or handle as needed
                            if (departments.isNotEmpty()) {
                                tokenManager.saveUserDepartments(departments.joinToString(", "))
                            }

                            Log.d("ddf", "sucess,$departments")
                            updateAdapterData(departments)
                        }
                        is DoctorViewModel.Resource.Error -> {
                            Log.d("ddf", "${state.message}")
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                        }
                        is DoctorViewModel.Resource.Idle -> {
                            // Do nothing
                        }
                    }
                }
            }
        }
    }

    private fun updateAdapterData(departments: List<String>) {
        departmentAdapter.updateDepartments(departments)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
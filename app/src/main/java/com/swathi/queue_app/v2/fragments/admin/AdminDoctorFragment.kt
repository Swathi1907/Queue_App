package com.swathi.queue_app.v2.fragments.admin

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.swathi.queue_app.R
import com.swathi.queue_app.databinding.NewAdminDoctorsBinding
import com.swathi.queue_app.v2.adapter.doctor.DoctorDirectoryAdapter
import com.swathi.queue_app.v2.models.DoctorDirectoryItem
import com.swathi.queue_app.v2.utilis.TokenManager
import com.swathi.queue_app.v2.viewmodels.AdminDashboardViewModel

class AdminDoctorsFragment : Fragment() {

    private var _binding: NewAdminDoctorsBinding? = null
    private val binding get() = _binding!!

    private lateinit var doctorAdapter: DoctorDirectoryAdapter
    private val viewModel: AdminDashboardViewModel by viewModels()
    private lateinit var tokenManager: TokenManager

    // Master list from API and currently filtered list
    private var allDoctorsList = listOf<DoctorDirectoryItem>()
    private var filteredDoctorsList = listOf<DoctorDirectoryItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = NewAdminDoctorsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tokenManager = TokenManager(requireContext())
        binding.btnAddDoctor.setOnClickListener {
            val scanResumeFragment = ScanResumeFragment()
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.nav_admin_graph, scanResumeFragment) // Update with your actual container layout ID if different
                .addToBackStack(null)
                .commit()
        }
        setupRecyclerView()
        setupFilters()
        observeViewModel()
        fetchDoctorDirectory()
    }

    private fun setupRecyclerView() {
        doctorAdapter = DoctorDirectoryAdapter(
            doctorList = emptyList(),
            onViewQueueClick = { doctor ->
                Toast.makeText(requireContext(), "View Queue for ${doctor.name}", Toast.LENGTH_SHORT).show()
            },
            onEditClick = { doctor ->
                Toast.makeText(requireContext(), "Edit Doctor: ${doctor.name}", Toast.LENGTH_SHORT).show()
            }
        )

        binding.rvDoctors.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = doctorAdapter
        }
    }

    private fun setupFilters() {
        val statusOptions = arrayOf("All Status", "Active", "Paused", "Off Duty")
        val statusAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, statusOptions)
        binding.spinnerStatus.adapter = statusAdapter

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterDoctors()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        val filterListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                filterDoctors()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.spinnerDepartment.onItemSelectedListener = filterListener
        binding.spinnerStatus.onItemSelectedListener = filterListener
    }

    private fun observeViewModel() {
        // Observe doctor directory live data from the ViewModel
        viewModel.doctorDirectory.observe(viewLifecycleOwner) { doctors ->
            allDoctorsList = doctors ?: emptyList()
            populateDepartmentSpinner()
            filterDoctors()
        }

        // Observe errors
        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMsg ->
            if (!errorMsg.isNullOrEmpty()) {
                Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchDoctorDirectory() {
        val hospitalId = tokenManager.getHospitalId() ?: ""
        if (hospitalId.isNotEmpty()) {
            viewModel.fetchDoctorDirectory(hospitalId)
        } else {
            Toast.makeText(requireContext(), "Hospital ID missing in session", Toast.LENGTH_SHORT).show()
        }
    }

    private fun populateDepartmentSpinner() {
        val rawDepartments = tokenManager.getUserDepartments()
        val hospitalDepartments = (rawDepartments as? List<*>)?.filterIsInstance<String>() ?: emptyList()

        // Use a Set to avoid duplicates while combining official departments and active doctor specializations
        val departments = mutableSetOf("All Departments")
        departments.addAll(hospitalDepartments)

        allDoctorsList.forEach { doc ->
            if (doc.specialization.isNotBlank()) {
                departments.add(doc.specialization)
            }
        }

        val deptAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, departments.toList())
        binding.spinnerDepartment.adapter = deptAdapter
    }
    private fun filterDoctors() {
        val searchQuery = binding.etSearch.text.toString().trim().lowercase()
        val selectedDept = binding.spinnerDepartment.selectedItem?.toString() ?: "All Departments"
        val selectedStatus = binding.spinnerStatus.selectedItem?.toString() ?: "All Status"

        filteredDoctorsList = allDoctorsList.filter { doctor ->
            val matchesSearch = searchQuery.isEmpty() ||
                    doctor.name.lowercase().contains(searchQuery) ||
                    doctor.doctorCode.lowercase().contains(searchQuery)

            val matchesDept = selectedDept == "All Departments" ||
                    doctor.specialization.equals(selectedDept, ignoreCase = true)

            // Convert UI status string (e.g. "Off Duty") to match backend format ("OFF_DUTY")
            val formattedStatus = selectedStatus.uppercase().replace(" ", "_")
            val matchesStatus = selectedStatus == "All Status" ||
                    doctor.status.uppercase() == formattedStatus

            matchesSearch && matchesDept && matchesStatus
        }

        doctorAdapter.updateData(filteredDoctorsList)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
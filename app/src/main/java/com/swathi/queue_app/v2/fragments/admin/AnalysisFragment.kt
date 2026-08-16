package com.swathi.queue_app.v2.fragments.admin

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import com.swathi.queue_app.R
import com.swathi.queue_app.databinding.AdminDoctorAnalysisBinding
import com.swathi.queue_app.v2.models.RegisterDoctorRequest
import com.swathi.queue_app.v2.models.ResumeScanResponse
import com.swathi.queue_app.v2.utilis.TokenManager
import com.swathi.queue_app.v2.viewmodels.AuthViewModel
import kotlinx.coroutines.launch

class AIAnalysisFragment : Fragment() {

    private var _binding: AdminDoctorAnalysisBinding? = null
    private val binding get() = _binding!!

    private var scanResponse: ResumeScanResponse? = null
    private lateinit var tokenManager: TokenManager

    private val selectedDepartments =
        mutableSetOf<String>()
    private lateinit var authViewModel: AuthViewModel

    companion object {
        private const val ARG_DOCTOR_DATA = "arg_doctor_data"

        fun newInstance(data: ResumeScanResponse): AIAnalysisFragment {
            val fragment = AIAnalysisFragment()
            val args = Bundle()
            args.putParcelable(ARG_DOCTOR_DATA, data)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            @Suppress("DEPRECATION")
            scanResponse = it.getParcelable(ARG_DOCTOR_DATA) as? ResumeScanResponse
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AdminDoctorAnalysisBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tokenManager = TokenManager(requireContext())
        setupClickListeners()
        authViewModel =
            ViewModelProvider(this)[AuthViewModel::class.java]
        val doctorData = scanResponse?.data
        if (scanResponse?.success == true && doctorData != null) {
            populateData(doctorData)
        } else {
            populateEmptyState()
        }
        setupDepartments()

        observeRegisterDoctor()
    }
    private fun observeRegisterDoctor() {

        lifecycleScope.launch {

            authViewModel.registerDoctorState.collect { state ->

                when (state) {

                    is AuthViewModel.Resource.Loading -> {

                        binding.btnConfirm.isEnabled = false
                        binding.btnConfirm.text = "ADDING DOCTOR..."
                    }

                    is AuthViewModel.Resource.Success -> {

                        binding.btnConfirm.isEnabled = true
                        binding.btnConfirm.text =
                            "CONFIRM & ADD DOCTOR"
Log.d("analysis","${state.data.message}")
                        Toast.makeText(
                            requireContext(),
                            state.data.message,
                            Toast.LENGTH_SHORT
                        ).show()

                        // Registration successful
                        requireActivity()
                            .supportFragmentManager
                            .beginTransaction()
                            .replace(
                                R.id.nav_admin_graph,
                                AdminDoctorsFragment()
                            )
                            .commit()
                    }

                    is AuthViewModel.Resource.Error -> {

                        binding.btnConfirm.isEnabled = true
                        binding.btnConfirm.text =
                            "CONFIRM & ADD DOCTOR"
                        Log.d("analysis","${state.message}")
                        Toast.makeText(
                            requireContext(),
                            state.message,
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    null -> {
                        // Nothing
                    }
                }
            }
        }
    }
    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding.btnConfirm.setOnClickListener {
            confirmAndAddDoctor()
        }
        binding.btnDiscard.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }
    private fun confirmAndAddDoctor() {

        if (selectedDepartments.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "Please select at least one department",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val doctorData = scanResponse?.data

        if (doctorData == null) {
            Toast.makeText(
                requireContext(),
                "Doctor data is unavailable",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val hospitalId = tokenManager.getHospitalId()

        if (hospitalId.isNullOrEmpty()) {
            Toast.makeText(
                requireContext(),
                "Hospital ID not found",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Get values from editable fields
        val email = binding.etDoctorEmail.text
            ?.toString()
            ?.trim()

        val phoneNumber = binding.etDoctorPhone.text
            ?.toString()
            ?.trim()

        val password = binding.etDoctorPassword.text
            ?.toString()
            ?.trim()

        // Validate email
        if (email.isNullOrEmpty()) {
            binding.etDoctorEmail.error = "Email is required"
            return
        }

        // Validate phone
        if (phoneNumber.isNullOrEmpty()) {
            binding.etDoctorPhone.error = "Phone number is required"
            return
        }

        // Validate password
        if (password.isNullOrEmpty()) {
            binding.etDoctorPassword.error = "Password is required"
            return
        }

        val request = RegisterDoctorRequest(
            name = doctorData.name,
            email = email,
            phoneNumber = phoneNumber,   // IMPORTANT
            password = password,
            hospitalId = hospitalId,
            departments = selectedDepartments.toList(),
            qualification = doctorData.qualification,
            rating = doctorData.rating ?: 5.0
        )

        Log.d(
            "AIAnalysis",
            "Register request = $request"
        )

        authViewModel.registerDoctor(request)
    }
    private fun setupDepartments() {

        binding.chipGroupDepartments.removeAllViews()
        selectedDepartments.clear()

        val hospitalDepartments =
            tokenManager.getHospitalDepartments()

        Log.d(
            "AIAnalysis",
            "Departments from TokenManager: $hospitalDepartments"
        )

        if (hospitalDepartments.isEmpty()) {
            binding.tvMatchSummary.text =
                "No hospital departments are available."
            return
        }

        for (department in hospitalDepartments) {

            val chip = Chip(requireContext()).apply {

                text = department
                isCheckable = true
                isClickable = true
                isChecked = false

                setEnsureMinTouchTargetSize(false)

                setOnCheckedChangeListener { _, checked ->

                    if (checked) {
                        selectedDepartments.add(department)
                    } else {
                        selectedDepartments.remove(department)
                    }

                    Log.d(
                        "AIAnalysis",
                        "Selected departments: $selectedDepartments"
                    )
                }
            }

            binding.chipGroupDepartments.addView(chip)
        }
    }
    private fun populateData(data: com.swathi.queue_app.v2.models.DoctorResumeData) {
        // 1. Basic Candidate Info
        binding.tvDoctorName.text = data.name ?: "Unknown Candidate"
        binding.tvDoctorQualification.text = data.qualification ?: "Qualification Pending"
        binding.etDoctorEmail.setText(data.email ?: "")
        binding.etDoctorPhone.setText(
            data.phoneNumber ?: ""
        )
        val phone = data.phoneNumber
            ?.filter { it.isDigit() }
            ?: ""

        binding.etDoctorPassword.setText(data.email+phone)
        // 2. Candidate Match Score & Summary
        binding.tvMatchScore.text = data.matchScore?.let { String.format("%.1f", it) } ?: "N/A"
        binding.tvMatchSummary.text = data.matchSummary ?: "No summary provided by AI analysis."

        // 3. Dynamically add extracted specializations into ChipGroup
        binding.chipGroupSpecializations.removeAllViews()
        val specs = data.specializations
        if (!specs.isNullOrEmpty()) {
            for (spec in specs) {
                val chip = Chip(requireContext()).apply {
                    text = spec
                    setChipBackgroundColorResource(android.R.color.white)
                    setTextColor(resources.getColor(android.R.color.holo_blue_dark, null))
                    isCheckable = false
                    isClickable = false
                    setEnsureMinTouchTargetSize(false)
                }
                binding.chipGroupSpecializations.addView(chip)
            }
        }

    }

    private fun populateEmptyState() {
        binding.tvDoctorName.text = "No Data Available"
        binding.tvDoctorQualification.text = "-"
        binding.tvMatchScore.text = "0.0"
        binding.tvMatchSummary.text = "No candidate scan data was received for analysis."
        binding.chipGroupSpecializations.removeAllViews()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }



}
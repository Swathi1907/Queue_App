package com.swathi.queue_app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.swathi.queue_app.databinding.FragmentHospitalDetailsBinding
import com.swathi.queue_app.viewmodel.HomeViewModel
import kotlin.getValue
import com.swathi.queue_app.viewmodel.admin.dashboardViewModel
import com.swathi.queue_app.adapter.DoctorAdapter

class HospDetailFrag : Fragment() {

    private var _binding: FragmentHospitalDetailsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: dashboardViewModel by viewModels()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentHospitalDetailsBinding.inflate(
            inflater,
            container,
            false
        )

        return binding.root
    }
    private lateinit var hospitalId: String
    private lateinit var hospitalName: String
    private lateinit var hospitalAddress: String
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        hospitalId = arguments?.getString("hospitalId") ?: ""
        hospitalName = arguments?.getString("hospitalName") ?: ""
        hospitalAddress = arguments?.getString("hospitalAddress") ?: ""

        binding.tvHospitalName.text = hospitalName
        binding.tvAddress.text = hospitalAddress

        binding.rvDoctors.layoutManager =
            LinearLayoutManager(requireContext())

        viewModel.doctorResponse.observe(viewLifecycleOwner) { response ->

            binding.tvHospitalName.text = response.hospital.hospitalName
            binding.tvAddress.text = response.hospital.address

            binding.rvDoctors.adapter =
                DoctorAdapter(response.doctors)
        }

        viewModel.getDoctors(hospitalId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
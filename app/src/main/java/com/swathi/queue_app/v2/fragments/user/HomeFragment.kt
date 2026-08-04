package com.swathi.queue_app.v2.fragments.user

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.swathi.queue_app.R
import com.swathi.queue_app.v2.adapter.HospitalAdapter
import com.swathi.queue_app.databinding.UserHomeScreenBinding
import com.swathi.queue_app.v2.viewmodels.HospitalViewModel
class HomeFragment : Fragment() {

    private var _binding: UserHomeScreenBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: HospitalViewModel
    private lateinit var hospitalAdapter: HospitalAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = UserHomeScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupViewModel()
        observeViewModel()

        // Trigger fetching hospitals via ViewModel
        viewModel.loadHospitals()
    }

    private fun setupRecyclerView() {
        hospitalAdapter = HospitalAdapter(emptyList()) { hospital ->
        // 1. Create a bundle to pass the hospital ID directly
        val bundle = Bundle().apply {
            putString("hospitalId", hospital._id)
        }
Log.d("homeuser","${hospital._id}")
        // 2. Instantiate UserHospitalFragment and attach arguments
        val hospitalFragment = UserHospitalFragment().apply {
            arguments = bundle
        }

        // 3. Perform the fragment transaction to navigate
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, hospitalFragment) // Make sure this matches your container ID
            .addToBackStack(null)
            .commit()
    }

        binding.rvNearbyHospitals.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = hospitalAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupViewModel() {
        // ViewModel handles instantiation of its internal dependencies safely
        viewModel = ViewModelProvider(this)[HospitalViewModel::class.java]
    }

    private fun observeViewModel() {
        viewModel.hospitals.observe(viewLifecycleOwner) { hospitalList ->
            hospitalAdapter.updateData(hospitalList)
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            Log.d("userhome","${errorMessage}");
            Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
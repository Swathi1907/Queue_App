package com.swathi.queue_app.v2.fragments.doctor

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.swathi.queue_app.databinding.DoctorProfileBinding
import com.swathi.queue_app.v2.ui.auth.LoginActivity
import com.swathi.queue_app.v2.utilis.TokenManager
import com.swathi.queue_app.v2.viewmodels.DoctorViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DoctorProfileFragment : Fragment() {

    private var _binding: DoctorProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DoctorViewModel by viewModels()
    private val tokenManager by lazy { TokenManager(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DoctorProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeProfileState()

        // Fetch doctor profile details upon screen load
        viewModel.fetchDoctorProfile()
        binding.layoutLogout.setOnClickListener {
            // Clear saved tokens/session data
            tokenManager.clearSession()

            // Navigate back to LoginActivity and clear task stack
            val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            requireActivity().finish()
        }
    }

    private fun observeProfileState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.doctorProfileState.collectLatest { state ->
                    when (state) {
                        is DoctorViewModel.Resource.Loading -> {
                            // Show loading state if needed
                        }
                        is DoctorViewModel.Resource.Success -> {
                            val profile = state.data.data
                            if (profile != null) {
                                binding.tvDoctorName.text = profile.name ?: "Dr. Unknown"
                                binding.tvDoctorPhone.text = "${profile.phoneNumber ?: "N/A"}"
                                binding.tvDoctorCode.text = "${profile.doctorCode ?: "N/A"}"
                                binding.tvDoctorRating.text = "${profile.rating ?: "4.5"}"

                                val departments = profile.department ?: emptyList()
                                binding.tvDepartmentsValue.text = departments.joinToString(separator = ", ")
                            }
                        }
                        is DoctorViewModel.Resource.Error -> {

                            Log.e("DoctorProfileFragment", "Error fetching profile: ${state.message}")
                        }
                        is DoctorViewModel.Resource.Idle -> {
                            // Do nothing
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
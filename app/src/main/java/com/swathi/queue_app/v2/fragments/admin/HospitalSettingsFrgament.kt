package com.swathi.queue_app.v2.fragments.admin

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.swathi.queue_app.databinding.NewAdminSettingsBinding
import com.swathi.queue_app.v2.models.HospitalAddress
import com.swathi.queue_app.v2.models.HospitalUpdateRequest
import com.swathi.queue_app.v2.utilis.TokenManager
import com.swathi.queue_app.v2.viewmodels.HospitalViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class HospitalSettingsFragment : Fragment() {

    private var _binding: NewAdminSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HospitalViewModel by viewModels()

    private var selectedBannerUri: Uri? = null
    private var uploadedBannerUrl: String? = null

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                selectedBannerUri = it
                binding.ivHospitalBanner.setImageURI(it)
                uploadBannerImageToImageKit(it)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = NewAdminSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val hospitalId = TokenManager(requireContext()).getHospitalId() ?: ""
        if (hospitalId.isNotEmpty()) {
            viewModel.loadHospitalById(hospitalId)
        }

        binding.ivHospitalBanner.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.btnSaveHospitalDetails.setOnClickListener {
            saveHospitalDetails()
        }

        observeViewModel()
    }

    private fun saveHospitalDetails() {
        val name = binding.etHospitalName.text.toString().trim()
        val description = binding.etHospitalDescription.text.toString().trim()
        val phone = binding.etHospitalPhone.text.toString().trim()
        val addressText = binding.etHospitalAddress.text.toString().trim()

        // Convert comma-separated departments string back to a List<String>
        val departmentsString = binding.etDepartments.text.toString().trim()
        val departmentsList = if (departmentsString.isNotEmpty()) {
            departmentsString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        } else {
            emptyList()
        }

        if (name.isEmpty()) {
            binding.etHospitalName.error = "Hospital name is required"
            return
        }

        val hospitalId = TokenManager(requireContext()).getHospitalId() ?: ""

        // Split the single address input or assign valid sub-components
        // to satisfy the backend validation for city and state requirements.
        // Example: Splitting by comma if user enters "Street, City, State, Zip"
        val addressParts = addressText.split(",").map { it.trim() }

        val streetVal = addressParts.getOrNull(0) ?: addressText
        val cityVal = addressParts.getOrNull(1).takeIf { !it.isNullOrBlank() } ?: "Patna"
        val stateVal = addressParts.getOrNull(2).takeIf { !it.isNullOrBlank() } ?: "Bihar"
        val zipCodeVal = addressParts.getOrNull(3) ?: ""

        val hospitalAddress = HospitalAddress(
            street = streetVal,
            city = cityVal,
            state = stateVal,
            zipCode = zipCodeVal
        )

        val request = HospitalUpdateRequest(
            hospitalId = hospitalId,
            hospitalName = name,
            description = description,
            phone = phone,
            address = hospitalAddress, // Now correctly passed as HospitalAddress? object
            departments = departmentsList,
            bannerImageUrl = uploadedBannerUrl
        )

        viewModel.saveHospitalDetails(request)
    }

    private fun uploadBannerImageToImageKit(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val context = requireContext()
                val inputStream = context.contentResolver.openInputStream(uri)
                val file =
                    File(context.cacheDir, "hospital_banner_${System.currentTimeMillis()}.jpg")

                inputStream?.use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }

                file.delete()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun observeViewModel() {
        viewModel.hospitalDetail.observe(viewLifecycleOwner) { response ->

            response?.data?.let { hospital ->

                binding.etHospitalName.setText(
                    hospital.name
                )

                binding.etHospitalDescription.setText(
                    hospital.description ?: ""
                )

                binding.etHospitalPhone.setText(
                    hospital.contactNumber ?: ""
                )


                // Address
                hospital.address?.let { addr ->

                    val fullAddress =
                        listOfNotNull(
                            addr.street,
                            addr.city,
                            addr.state,
                            addr.zipCode
                        )
                            .filter { part ->
                                part.isNotBlank()
                            }
                            .joinToString(", ")

                    binding.etHospitalAddress.setText(
                        fullAddress
                    )
                }


                binding.etHospitalEmail.setText(
                    hospital.email ?: ""
                )

                binding.etHospitalCode.setText(
                    hospital.code ?: ""
                )


                // =================================================
                // HOSPITAL DEPARTMENTS
                // =================================================

                val departmentsList =
                    hospital.departments ?: emptyList()


                // Show in settings EditText
                binding.etDepartments.setText(
                    departmentsList.joinToString(", ")
                )


                // Save to TokenManager
                val tokenManager =
                    TokenManager(requireContext())

                tokenManager.saveHospitalDepartments(
                    departmentsList
                )


                Log.d(
                    "HospitalSettings",
                    "Hospital departments saved: $departmentsList"
                )


                uploadedBannerUrl =
                    hospital.imageUrl
            }
        }

        viewModel.updateResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess { response ->
                Log.d("hospset","${response.message}");
                Toast.makeText(requireContext(), response.message, Toast.LENGTH_SHORT).show()
            }.onFailure { error ->
                Toast.makeText(
                    requireContext(),
                    error.message ?: "Update failed",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
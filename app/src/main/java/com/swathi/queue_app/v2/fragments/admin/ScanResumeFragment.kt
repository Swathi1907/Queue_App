package com.swathi.queue_app.v2.fragments.admin

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.swathi.queue_app.R
import com.swathi.queue_app.databinding.UploadResumeBinding
import com.swathi.queue_app.v2.models.DoctorResumeData
import com.swathi.queue_app.v2.models.Education
import com.swathi.queue_app.v2.models.Experience
import com.swathi.queue_app.v2.viewmodels.DoctorViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream

class ScanResumeFragment : Fragment() {

    private var _binding: UploadResumeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DoctorViewModel by viewModels()

    private var selectedFileUri: Uri? = null
    private var selectedFileName: String = ""

    // Replace with your dynamic hospital ID logic (e.g., from arguments or session prefs)
    private val currentHospitalId = "A-5198"

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedFileUri = uri
            selectedFileName = getFileName(uri)
            binding.tvBrowse.text = "Selected: $selectedFileName"
            Toast.makeText(requireContext(), "File selected: $selectedFileName", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = UploadResumeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { requireActivity().onBackPressed() }

        binding.btnSelectFile.setOnClickListener {
            filePickerLauncher.launch("application/pdf")
        }

        binding.btnScanResume.setOnClickListener {
            val uri = selectedFileUri
            if (uri == null) {
                Toast.makeText(requireContext(), "Please select a resume file first.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            uploadAndScanResume(uri)
        }
    }

    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            requireContext().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        result = cursor.getString(index)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result ?: "resume.pdf"
    }

    private fun readBytesFromUri(uri: Uri): ByteArray? {
        return try {
            requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                ByteArrayOutputStream().use { byteBuffer ->
                    val buffer = ByteArray(1024)
                    var len: Int
                    while (inputStream.read(buffer).also { len = it } != -1) {
                        byteBuffer.write(buffer, 0, len)
                    }
                    byteBuffer.toByteArray()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun uploadAndScanResume(uri: Uri) {
        val fileBytes = readBytesFromUri(uri)
        if (fileBytes == null) {
            Toast.makeText(requireContext(), "Failed to read file bytes.", Toast.LENGTH_SHORT).show()
            return
        }

        val requestFile = fileBytes.toRequestBody("application/pdf".toMediaTypeOrNull())
        val resumePart = MultipartBody.Part.createFormData("resume", selectedFileName, requestFile)

        viewModel.scanDoctorResume(currentHospitalId, resumePart)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.resumeScanState.collectLatest { state ->
                    when (state) {
                        is DoctorViewModel.Resource.Idle -> {
                            // Do nothing
                        }
                        is DoctorViewModel.Resource.Loading -> {
                            Toast.makeText(requireContext(), "Scanning resume with AI...", Toast.LENGTH_SHORT).show()
                            binding.btnScanResume.isEnabled = false
                        }
                        is DoctorViewModel.Resource.Success -> {
                            binding.btnScanResume.isEnabled = true
                            val response = state.data
                            if (response.success && response.data != null) {
                                Toast.makeText(requireContext(), "Resume scanned successfully!", Toast.LENGTH_SHORT).show()

                                // Directly pass the whole ResumeScanResponse parcelable object
                                val analysisFragment = AIAnalysisFragment.newInstance(response)
                                requireActivity().supportFragmentManager.beginTransaction()
                                    .replace(R.id.nav_admin_graph, analysisFragment)

                                    .commit()

                            } else {
                                val availableDepts = response.data?.availableDepartments?.joinToString(", ") ?: "None"
                                Toast.makeText(
                                    requireContext(),
                                    "${response.message}\nAvailable: $availableDepts",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                        is DoctorViewModel.Resource.Error -> {
                            binding.btnScanResume.isEnabled = true
                            Toast.makeText(requireContext(), "Error: ${state.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
        parentFragmentManager.setFragmentResultListener(
            "DISCARD_RESUME",
            viewLifecycleOwner
        ) { _, _ ->

            selectedFileUri = null

            // Clear the UI
            binding.tvBrowse.text = ""
            binding.btnScanResume.isEnabled = false

            Log.d("RESUME_SCAN", "Selected file cleared after discard")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
package com.swathi.queue_app.fragments

import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
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
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.swathi.queue_app.R
import com.swathi.queue_app.model.DoctorModel
import android.widget.EditText
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.swathi.queue_app.model.EditRequest
import java.util.Calendar

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
    private var isAdmin = false
    private val days = arrayOf(
        "Monday",
        "Tuesday",
        "Wednesday",
        "Thursday",
        "Friday",
        "Saturday",
        "Sunday"
    )

    private val checkedDays = BooleanArray(days.size)
    private val selectedDays = mutableListOf<String>()
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
         isAdmin = arguments?.getBoolean("isAdmin", false) ?: false
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
            Glide.with(requireContext())
                .load(response.hospital.hospitalImage)
                .placeholder(R.drawable.img)
                .error(R.drawable.img)
                .into(binding.imgHospital)
            binding.rvDoctors.adapter =
                DoctorAdapter(
                    doctorList = response.doctors,
                    isAdmin,
                    onEdit = { doctor ->
                        // Open edit dialog here
                        showEditDoctorDialog(doctor)
                    },
                    onDelete = { doctor ->
                     //   viewModel.deleteDoctor(doctor.id)
                    }
                )
           // viewModel.getDoctors(hospitalId)
        }

        viewModel.getDoctors(hospitalId)

        viewModel.updateDoctorResponse.observe(viewLifecycleOwner) {

            Toast.makeText(
                requireContext(),
                it.message,
                Toast.LENGTH_SHORT
            ).show()

            viewModel.getDoctors(hospitalId)
        }
    }

    private fun showEditDoctorDialog(doctor: DoctorModel) {

        val dialogView = layoutInflater.inflate(
            R.layout.manage_doctor,
            null
        )

        val etDoctorName =
            dialogView.findViewById<EditText>(R.id.etDoctorName)

        val etSpecialization =
            dialogView.findViewById<EditText>(R.id.etSpecialization)

        val etQualification =
            dialogView.findViewById<EditText>(R.id.etQualification)

        val etRoomNumber =
            dialogView.findViewById<EditText>(R.id.etRoomNumber)

        val etAvailableDays =
            dialogView.findViewById<TextInputEditText>(R.id.etAvailableDays)

        val etStartTime =
            dialogView.findViewById<TextInputEditText>(R.id.etStartTime)

        val etEndTime =
            dialogView.findViewById<TextInputEditText>(R.id.etEndTime)

        // Prefill fields
        etDoctorName.setText(doctor.doctorName)
        etSpecialization.setText(doctor.specialization)
        etQualification.setText(doctor.qualification)
        etRoomNumber.setText(doctor.roomNumber)
        etStartTime.setText(doctor.startTime)
        etEndTime.setText(doctor.endTime)

        selectedDays.clear()
        selectedDays.addAll(doctor.availableDays)

        checkedDays.fill(false)

        doctor.availableDays.forEach { day ->
            val index = days.indexOf(day)
            if (index != -1) {
                checkedDays[index] = true
            }
        }

        etAvailableDays.setText(
            doctor.availableDays.joinToString(", ")
        )

        etStartTime.setOnClickListener {
            showTimePicker(etStartTime)
        }

        etEndTime.setOnClickListener {
            showTimePicker(etEndTime)
        }

        etAvailableDays.setOnClickListener {

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select Available Days")
                .setMultiChoiceItems(days, checkedDays) { _, which, isChecked ->
                    checkedDays[which] = isChecked
                }
                .setPositiveButton("OK") { _, _ ->

                    selectedDays.clear()

                    for (i in days.indices) {
                        if (checkedDays[i]) {
                            selectedDays.add(days[i])
                        }
                    }
                    etAvailableDays.setText(
                        selectedDays.joinToString(", ")
                    )
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
        val titleView = layoutInflater.inflate(
            R.layout.manage_doctor_title,
            null
        )

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setCustomTitle(titleView)
            .setView(dialogView)
            .create()

        dialog.show()

        val btnUpdate =
            dialogView.findViewById<MaterialButton>(R.id.btnUpdate)

        val btnCancel =
            dialogView.findViewById<MaterialButton>(R.id.btnCancel)

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnUpdate.setOnClickListener {

            Log.d("UPDATE", "Update clicked")

            viewModel.updateDoctor(
                EditRequest(
                    doctorName = etDoctorName.text.toString().trim(),
                    specialization = etSpecialization.text.toString().trim(),
                    qualification = etQualification.text.toString().trim(),
                    roomNumber = etRoomNumber.text.toString().trim().toInt(),
                    availableDays = selectedDays,
                    startTime = etStartTime.text.toString().trim(),
                    endTime = etEndTime.text.toString().trim()
                ),
                doctor.id
            )

            dialog.dismiss()
        }

    }
    private fun showTimePicker(editText: TextInputEditText) {

        val calendar = Calendar.getInstance()

        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(
            requireContext(),
            { _, selectedHour, selectedMinute ->

                val amPm =
                    if (selectedHour >= 12) "PM" else "AM"

                var hour12 = selectedHour % 12
                if (hour12 == 0) hour12 = 12

                val time = String.format(
                    "%02d:%02d %s",
                    hour12,
                    selectedMinute,
                    amPm
                )

                editText.setText(time)

            },
            hour,
            minute,
            false
        ).show()
    }
override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
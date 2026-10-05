package com.swathi.queue_app.v2.fragments.user

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.swathi.queue_app.R
import com.swathi.queue_app.databinding.FragmentJoinQueueBinding

class JoinQueueFragment : Fragment() {

    private var _binding: FragmentJoinQueueBinding? = null
    private val binding get() = _binding!!

    // Doctor details received from previous screen
    private var doctorCode: String = ""
    private var consultationFeeINR: Int = 500
    private var hospitalId: String = ""

    private var doctorName: String = "Dr. Emilia Emelson"
    private var specialty: String = "General Practice"
    private var departmentName: String = ""

    private var waitTimeText: String = "🕒 Current Wait: ~45 mins"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let { bundle ->

            doctorCode =
                bundle.getString("DOCTOR_CODE", "")

            consultationFeeINR =
                bundle.getInt("CONSULTATION_FEE_INR", 500)

            doctorName =
                bundle.getString(
                    "DOCTOR_NAME",
                    "Dr. Emilia Emelson"
                )

            hospitalId =
                bundle.getString(
                    "HOSPITAL_CODE",
                    ""
                )

            departmentName =
                bundle.getString(
                    "DEPARTMENT_NAME",
                    ""
                )

            waitTimeText =
                bundle.getString(
                    "WAIT_TIME",
                    "🕒 Current Wait: ~45 mins"
                )
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentJoinQueueBinding.inflate(
            inflater,
            container,
            false
        )

        return binding.root
    }


    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        Log.d(
            "JOIN_QUEUE_FRAG",
            "Department: $departmentName"
        )


        // =========================
        // Doctor Header
        // =========================

        binding.tvDoctorName.text = doctorName

        binding.tvDoctorSpeciality.text =
            specialty



        // =========================
        // Confirm & Join
        // =========================

        binding.btnConfirmJoin.setOnClickListener {

            val symptoms =
                binding.etSymptoms.text
                    ?.toString()
                    ?.trim()
                    ?: ""


            // Optional validation
            if (symptoms.isEmpty()) {

                Toast.makeText(
                    requireContext(),
                    "Please describe your symptoms",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }


            // =========================
            // Pass data to Payment
            // =========================

            val bundle = Bundle().apply {

                putString(
                    "DOCTOR_CODE",
                    doctorCode
                )

                putInt(
                    "CONSULTATION_FEE_INR",
                    consultationFeeINR
                )

                putString(
                    "DOCTOR_NAME",
                    doctorName
                )

                putString(
                    "SPECIALTY",
                    specialty
                )

                putString(
                    "DEPARTMENT_NAME",
                    departmentName
                )

                putString(
                    "HOSPITAL_CODE",
                    hospitalId
                )

                putString(
                    "SYMPTOMS",
                    symptoms
                )
            }


            // =========================
            // Open Payment Fragment
            // =========================

            val paymentFragment =
                PaymentFragment().apply {

                    arguments = bundle
                }


            requireActivity()
                .supportFragmentManager
                .beginTransaction()
                .replace(
                    R.id.fragmentContainer,
                    paymentFragment
                )
                .addToBackStack(null)
                .commit()
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
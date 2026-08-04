package com.swathi.queue_app.v2.fragments.user

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.swathi.queue_app.R
import com.swathi.queue_app.databinding.FragmentJoinQueueBinding

class JoinQueueFragment : Fragment() {

    private var _binding: FragmentJoinQueueBinding? = null
    private val binding get() = _binding!!

    // Doctor details received from the previous doctor list screen
    private var doctorCode: String = ""
    private var consultationFeeINR: Int = 500
    private var doctorName: String = "Dr. Emilia Emelson"
    private var specialty: String = "General Practice"
    private var waitTimeText: String = "🕒 Current Wait: ~45 mins"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            doctorCode = it.getString("DOCTOR_CODE", "")
            consultationFeeINR = it.getInt("CONSULTATION_FEE_INR", 500)
            doctorName = it.getString("DOCTOR_NAME", "Dr. Emilia Emelson")
            specialty = it.getString("SPECIALTY", "General Practice")
            waitTimeText = it.getString("WAIT_TIME", "🕒 Current Wait: ~45 mins")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentJoinQueueBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Bind doctor header info
        binding.tvDoctorName.text = doctorName
        binding.tvSpecialty.text = specialty
        binding.tvWaitTime.text = waitTimeText

        // Handle button click to pass data over to the Payment Fragment
        binding.btnConfirmJoin.setOnClickListener {
            // Handle button click to pass data over to the Payment Fragment
            binding.btnConfirmJoin.setOnClickListener {
                val symptoms = binding.etSymptoms.text.toString().trim()

                // Bundle all the collected data (doctor details + user symptoms)
                val bundle = Bundle().apply {
                    putString("DOCTOR_CODE", doctorCode)
                    putInt("CONSULTATION_FEE_INR", consultationFeeINR)
                    putString("DOCTOR_NAME", doctorName)
                    putString("SPECIALTY", specialty)
                    putString("SYMPTOMS", symptoms)
                }

                // CORRECT: Instantiate JoinPaymentFragment (not JoinQueueFragment)
                val paymentFragment = PaymentFragment().apply {
                    arguments = bundle
                }

                // Perform manual fragment transaction to open the Payment Fragment
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, paymentFragment)
                    .addToBackStack(null)
                    .commit()


    }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
package com.swathi.queue_app.v2.fragments.user

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import com.swathi.queue_app.R
import com.swathi.queue_app.databinding.FragmentPaymentBinding
import com.swathi.queue_app.v2.models.PaymentVerifyRequest
import com.swathi.queue_app.v2.utilis.TokenManager
import com.swathi.queue_app.v2.viewmodels.HospitalViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONObject

class PaymentFragment : Fragment(), PaymentResultWithDataListener {

    private var _binding: FragmentPaymentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HospitalViewModel by viewModels()
    private lateinit var tokenManager: TokenManager

    private var doctorCode: String = ""
    private var consultationFeeINR: Int = 500
    private var departmentName: String = ""
    private var hospitalId: String = ""
    private var symptoms:String=""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tokenManager = TokenManager(requireContext())

        arguments?.let {
            doctorCode = it.getString("DOCTOR_CODE", "")
            consultationFeeINR = it.getInt("CONSULTATION_FEE_INR", 100)
            departmentName = it.getString("DEPARTMENT_NAME", "")
            hospitalId = it.getString("HOSPITAL_CODE", "")
            symptoms = it.getString("SYMPTOMS", "")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPaymentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Checkout.preload(requireContext().applicationContext)
        binding.tvFeeAmount.text = "₹ $consultationFeeINR"

        binding.btnPayNow.setOnClickListener {
            initiateRazorpayOrder(consultationFeeINR)
        }

        observePaymentState()
    }

    private fun initiateRazorpayOrder(amountInINR: Int) {
        val userEmail = tokenManager.getEmail() ?: "user@example.com"
        val userContact = tokenManager.getContact() ?: "9876543210"

        // Disable button safely to prevent multiple clicks
        _binding?.btnPayNow?.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Fetch real order ID dynamically from your backend via ViewModel
                val serverGeneratedOrderId = viewModel.createRazorpayOrder(amountInINR, doctorCode)

                // Safely update UI components once coroutine finishes
                _binding?.let { safeBinding ->
                    safeBinding.btnPayNow.isEnabled = true

                    if (!serverGeneratedOrderId.isNullOrEmpty()) {
                        Log.d("pf","starting razor payment")
                        startRazorpayPayment(serverGeneratedOrderId, amountInINR, userEmail, userContact)
                    } else {
                        if (isAdded) {
                            Toast.makeText(requireContext(), "Failed to generate payment order", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                _binding?.btnPayNow?.isEnabled = true
                if (isAdded) {
                    Toast.makeText(requireContext(), "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
                e.printStackTrace()
            }
        }
    }

    private fun startRazorpayPayment(orderId: String, amountInINR: Int, email: String, contact: String) {
        val currentActivity = activity ?: return
        val checkout = Checkout()
        checkout.setKeyID("rzp_test_TL8wCC2G31Lu5O")
//rzp_test_TL8wCC2G31Lu5O
        try {
            val options = JSONObject().apply {
                put("name", "Queue App")
                put("description", "Consultation Fee Payment")
                put("image", "https://yourserver.com/logo.png")
                put("order_id", orderId)
                put("currency", "INR")
                put("amount", amountInINR * 100)
                put("prefill.email", email)
                put("prefill.contact", contact)
                put("theme.color", "#0B3C5D")

            }

            // Ensure it opens on the main thread safely
            currentActivity.runOnUiThread {
                checkout.open(currentActivity, options)
            }
        } catch (e: Exception) {
            Log.e("RazorpayCrash", "Failed to open checkout: ${e.localizedMessage}", e)
            if (isAdded) {
                Toast.makeText(requireContext(), "Error opening payment gateway: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        val orderId = paymentData?.orderId ?: ""
        val signature = paymentData?.signature ?: ""
        val paymentId = razorpayPaymentId ?: paymentData?.paymentId ?: ""

        if (orderId.isEmpty() || signature.isEmpty() || paymentId.isEmpty()) {
            if (isAdded) {
                Toast.makeText(requireContext(), "Payment data missing from Razorpay callback", Toast.LENGTH_SHORT).show()
            }
            return
        }

        Log.d("RazorpaySuccess", "Payment ID: $paymentId, Order ID: $orderId, Signature: $signature")

        val userId = tokenManager.getUserId() ?: ""
        val patientName = tokenManager.getUserName() ?: "Patient"

        val verifyRequest = PaymentVerifyRequest(
            razorpay_order_id = orderId,
            razorpay_payment_id = paymentId,
            razorpay_signature = signature,
            doctorCode = doctorCode,
            hospitalId = hospitalId,
            department = departmentName,
            userId = userId,
            patientName = patientName,
            amount = consultationFeeINR,
            notes = symptoms
        )

        viewModel.verifyPayment(verifyRequest)

        // INSTANT NAVIGATION: Pop right back to your Queues Fragment safely

    }

    private fun observePaymentState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.paymentVerificationState.collectLatest { resource ->
                if (!isAdded) return@collectLatest

                when (resource) {
                    is HospitalViewModel.Resource.Loading -> {
                        // Show progress bar if needed
                    }
                    is HospitalViewModel.Resource.Success -> {
                        val tokenNumber = resource.data
                        Toast.makeText(
                            requireContext(),
                            "Queue Confirmed! Token Number: $tokenNumber",
                            Toast.LENGTH_LONG
                        ).show()
                        try {
                            // Pop back stack cleanly to your queues fragment destination ID
                            findNavController().popBackStack(R.id.nav_queues, false)

                        } catch (e: Exception) {
                            Log.e("NavigationError", "Failed to navigate after payment: ${e.message}")
                            requireActivity().onBackPressedDispatcher.onBackPressed()
                        }
                    }
                    is HospitalViewModel.Resource.Error -> {
                        Toast.makeText(
                            requireContext(),
                            "Verification Failed: ${resource.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        Log.d("payfrag", "${resource.message}")
                    }
                    is HospitalViewModel.Resource.Idle -> {}
                }
            }
        }
    }

    override fun onPaymentError(code: Int, response: String?, paymentData: PaymentData?) {
        Log.e("RazorpayError", "Error Code: $code, Response: $response")
        if (isAdded) {
            Toast.makeText(requireContext(), "Payment Failed: $response", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
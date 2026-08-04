package com.swathi.queue_app.v2.fragments.user

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import com.swathi.queue_app.databinding.FragmentPaymentBinding
import org.json.JSONObject

class PaymentFragment : Fragment(), PaymentResultWithDataListener {

    private var _binding: FragmentPaymentBinding? = null
    private val binding get() = _binding!!

    // Dynamic variables passed from the previous doctor list screen
    private var doctorCode: String = ""
    private var consultationFeeINR: Int = 500 // Default fallback
    private var departmentName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Retrieve arguments passed from the doctor adapter/intent
        arguments?.let {
            doctorCode = it.getString("DOCTOR_CODE", "")
            consultationFeeINR = it.getInt("CONSULTATION_FEE_INR", 500)
            departmentName = it.getString("DEPARTMENT_NAME", "")
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

        // Preload Razorpay checkout for faster rendering
        Checkout.preload(requireContext().applicationContext)

        // Optional: Display the dynamic fee in your UI if you have a text view for it
        // binding.tvFee.text = "₹ $consultationFeeINR"

        binding.btnPayNow.setOnClickListener {
            // Use the dynamic consultation fee retrieved from arguments
            initiateRazorpayOrder(consultationFeeINR)
        }
    }

    private fun initiateRazorpayOrder(amountInINR: Int) {
        // TODO: Make your Retrofit backend call here to generate the order ID on your server:
        // POST /api/v2/payment/create-order with { amount: amountInINR, doctorCode: doctorCode }

        val serverGeneratedOrderId = "order_MOCK_123456" // Replace with actual backend response

        startRazorpayPayment(serverGeneratedOrderId, amountInINR)
    }

    private fun startRazorpayPayment(orderId: String, amountInINR: Int) {
        val checkout = Checkout()
        checkout.setKeyID("rzp_test_TL8wCC2G31Lu5O")

        try {
            val options = JSONObject().apply {
                put("name", "Queue App")
                put("description", "Consultation Fee Payment")
                put("image", "https://yourserver.com/logo.png")
                put("order_id", orderId)
                put("currency", "INR")
                put("amount", amountInINR * 100) // Convert INR to paise
                put("prefill.email", "user@example.com")
                put("prefill.contact", "9876543210")
                put("theme.color", "#0B3C5D")
            }

            checkout.open(requireActivity(), options)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error in payment: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        val orderId = paymentData?.orderId
        val signature = paymentData?.signature

        Log.d("RazorpaySuccess", "Payment ID: $razorpayPaymentId, Order ID: $orderId, Signature: $signature")

        // TODO: Send orderId, razorpayPaymentId, signature, and doctorCode to your backend verify route:
        // POST /api/v2/payment/verify

        Toast.makeText(requireContext(), "Payment Successful! ID: $razorpayPaymentId", Toast.LENGTH_LONG).show()
    }



    override fun onPaymentError(code: Int, response: String?, paymentData: PaymentData?) {
        Log.e("RazorpayError", "Error Code: $code, Response: $response")
        Toast.makeText(requireContext(), "Payment Failed: $response", Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
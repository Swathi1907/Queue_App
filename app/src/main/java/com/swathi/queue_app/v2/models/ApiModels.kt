package com.swathi.queue_app.v2.models

import com.google.gson.annotations.SerializedName

// --- Auth Models ---

data class LoginRequest(
    val email: String,
    val password: String,
    val role: String
)
data class Hospital(
    val _id: String,
    val name: String,
    val code: String,
    val contactNumber: String,
    val email: String?,
    val address:String,
    val departments: List<String>,
    val isActive: Boolean,
    val createdAt: String?,
    val updatedAt: String?
)

/*data class HospitalAddress(
    val street: String?,
    val city: String,
    val state: String,
    val zipCode: String?
)
*/
data class HospitalResponse(
    val success: Boolean,
    val count: Int,
    val data: List<Hospital>
)
data class SignupRequest(
    val name: String,
    val email: String?,
    val phoneNumber: String,
    val password: String
)

data class AuthResponse(
    val success: Boolean,
    val message: String,
    val data: UserData?
)

data class UserData(
    val _id: String,
    val name: String,
    val email: String?,
    val phoneNumber: String,
    val role: String, // Explicitly captures 'PATIENT', 'DOCTOR', 'COMPOUNDERS', or 'SUPER_ADMIN'
    val hospitalId: String?,
    val department: String?,
    val doctorCode: String?,
    val qualification: String?,
    val rating: Double?,
    val isAvailable: Boolean?,
    val isActive: Boolean?,
    val jwt_token: String?
)

// --- Payment Models ---

data class PaymentVerifyRequest(
    val razorpay_order_id: String,
    val razorpay_payment_id: String,
    val razorpay_signature: String
)

data class PaymentVerifyResponse(
    val success: Boolean,
    val message: String
)
data class DepartmentResponseWrapper(
    val success: Boolean,
    val data: DepartmentData?
)
data class CreateQueueRequest(
    val hospitalId: String,
    val department: String,
    val doctorCode: String
)

data class QueueResponseWrapper(
    val success: Boolean,
    val message: String,
    val data: QueueData?
)

data class QueueData(
    val _id: String,
    val hospitalId: String,
    val department: String,
    val doctorCode: String,
    val date: String,
    val tokens: List<Any>,
    val isActive: Boolean
)
data class DepartmentData(
    val hospitalCode: String,
    val hospitalName: String,
    val departments: List<String>
)
/*
data class DepartmentData(
    val hospitalCode: String,
    val hospitalName: String,
    val departments: List<DepartmentItem>
)

data class DepartmentItem(
    val name: String,
    val waitingCount: Int
) */
data class VerifyDoctorCodeRequest(
    val email: String,
    val password: String,
    val hospitalId: String,
    val doctorCode: String
)
data class VerifyHospitalRequest(
    @SerializedName("email")
    val email: String,

    @SerializedName("password")
    val password: String,

    @SerializedName("hospitalId")
    val hospitalId: String
)
data class VerifyDoctorCodeResponse(
    val success: Boolean,
    val message: String,
    val data: DoctorVerificationData?
)

data class DoctorVerificationData(
    val jwt_token: String,
    val role: String,
    val hospitalId: String,
    val doctorCode: String,
    val doctor: DoctorDetails?
)

data class DoctorDetails(
    val _id: String,
    val name: String,
    val department: List<String>,
    val qualification: String?,
    val isAvailable: Boolean
)
data class VerifyHospitalResponse(
    val success: Boolean,
    val requiresDoctorCode: Boolean?,
    val message: String,
    val data: HospitalVerifyData?
)

data class HospitalVerifyData(
    val jwt_token: String?,
    val role: String?,
    val hospitalId: String?,
    val user: UserDto?
)
data class DepartmentResponse(
    val success: Boolean,
    val count: Int,
    val message: String?,
    val data: List<String>
)

data class DoctorResponse(
    val success: Boolean,
    val count: Int,
    val message: String?,
    val data: List<Doctor>
)

data class Doctor(
    val _id: String,
    val name: String,
    val email: String,
    val department: List<String>,
    val hospitalId: String,
    val role: String,
    val doctorCode:String
)
data class UserDto(
    val _id: String?,
    val name: String?,
    val email: String?
)
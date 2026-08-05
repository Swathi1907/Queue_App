package com.swathi.queue_app.v2.models

import com.google.gson.annotations.SerializedName


// Next and complete requests
// --- Queue Action Request & Response Models ---

data class QueueActionRequest(
    val department: String,
    val doctorCode: String
)

data class QueueActionResponse(
    val success: Boolean,
    val message: String,
    val data: QueueActionData?
)

data class QueueActionData(
    val sessionId: String,
    val queueStatus: String,
    val tokens: List<QueueTokenItem>
)

data class QueueTokenItem(
    @SerializedName("_id") val id: String,
    val tokenNumber: Int,
    val userId: String,
    val patientName: String,
    val orderId: String,
    val paymentId: String,
    val amountPaid: Double,
    val status: String, // "WAITING", "IN_CONSULTATION", "COMPLETED", "CANCELLED"
    val createdAt: String
)


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
data class QueueDashboardResponse(
    val success: Boolean = false,
    val data: DashboardData? = null
)

data class DashboardData(
    val activeQueue: List<ActiveQueueDto> = emptyList(),
    val recentHistory: List<HistoryItemDto> = emptyList()
)

data class ActiveQueueDto(
    val queueId: String = "",
    val hospitalName: String = "",
    val hospitalLogoUrl: String? = null,
    val doctorDetails: String = "",
    val status: String = "",
    val peopleAheadText: String = "",
    val estWaitTimeText: String = "",
    val tokenNumber: Int = 0
)
data class DoctorProfileResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: DoctorData?
)
data class ActiveSessionResponse(
    val success: Boolean,
    val message: String,
    val data: SessionData?
)

data class SessionData(
    val sessionId: String,
    val queueStatus: String?, // "ACTIVE", "PAUSED", "CLOSED"
    val tokens: List<TokenItem>?
)
data class TokenItem(
    val tokenId: String?,
    val tokenNumber: String?, // Or Int?, depending on how token numbers are formatted (e.g., "A-124")
    val patientName: String?,
    val notes: String?,
    val status: String? // "WAITING", "IN_CONSULTATION", "COMPLETED", "CANCELLED"
)
data class DoctorData(
    @SerializedName("_id")
    val id: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("email")
    val email: String,
    @SerializedName("department")
    val department: List<String>?,

    @SerializedName("hospitalId")
    val hospitalId: String?,
    @SerializedName("rating")
    val rating: String?,

    @SerializedName("phoneNumber")
    val phoneNumber: String?,
    @SerializedName("doctorCode")
    val doctorCode: String?,

    @SerializedName("qualification")
    val qualification: String?,

    @SerializedName("isAvailable")
    val isAvailable: Boolean
)
data class HistoryItemDto(
    val queueId: String = "",
    val hospitalName: String = "",
    val subText: String = "",
    val date: String = ""
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
    val department: List<String>,
    val doctorCode: String?,
    val qualification: String?,
    val rating: Double?,
    val isAvailable: Boolean?,
    val isActive: Boolean?,
    val jwt_token: String?
)

// --- Payment Models ---

data class OrderCreateRequest(
    val amount: Int,
    val doctorCode: String
)

data class OrderCreateResponse(
    val success: Boolean,
    val orderId: String?,
    val amount: Int?,
    val currency: String?,
    val message: String?
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
data class PaymentVerifyRequest(
    val razorpay_order_id: String,
    val razorpay_payment_id: String,
    val razorpay_signature: String,
    val doctorCode: String,
    val hospitalId: String,
    val department: String,
    val userId: String,
    val patientName: String,
    val amount: Int
)

data class PaymentVerifyResponse(
    val success: Boolean,
    val message: String,
    val tokenNumber: Int?
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
/*data class DepartmentData(
    val hospitalCode: String,
    val hospitalName: String,
    val departments: List<String>
)*/

data class DepartmentData(
    val hospitalCode: String,
    val hospitalName: String,
    val departments: List<DepartmentItem>
)
data class HospitalDetailResponse(
    val success: Boolean,
    val data: HospitalDetailItem
)
// --- User-Side Doctor Display Models ---


data class UserDoctorResponse(
    val success: Boolean,
    val count: Int,
    val message: String?,
    val data: List<UserDoctorItem>
)

data class UserDoctorItem(
    @SerializedName("_id") val id: String,
    val doctorCode: String,
    val name: String,
    val specialty: String,
    val imageUrl: String?,
    val consultationFee: Double,
    val peopleAhead: Int,
    val estimatedWaitTime: String,
    @SerializedName("isJoined") val isJoined: Boolean = false,
    @SerializedName("isQueuePaused") val isQueuePaused: Boolean = false // Added property
)
data class HospitalDetailItem(
    @SerializedName("_id") val id: String,
    val name: String,
    val code: String,
    val address: String,
    val distance: String?,
    val rating: Double?,
    val reviewsCount: Int?,
    val waitTime: String?,
    val imageUrl: String?
)
data class DepartmentItem(
    val name: String,
    val waitingCount: Int
)
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
package com.swathi.queue_app.v2.models

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
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
data class QueueStatusUpdateRequest(
    val department: String,
    val doctorCode: String,
    val queueStatus: String // "ACTIVE", "PAUSED", "CLOSED"
)
/*data class QueueActionData(
    val sessionId: String,
    val queueStatus: String,
    val tokens: List<QueueTokenItem>
)
*/
data class QueueActionData(
    val sessionId: String,
    val queueStatus: String,
    val avgServiceTime: Int?,               // <-- Added to capture service time updates
    val calledToken: QueueTokenItem?,       // <-- Added for next() responses
    val completedToken: QueueTokenItem?,    // <-- Added for completeCurrent() responses
    val metrics: EtaMetrics?,               // <-- Added for call next ETA metrics
    val nextMetrics: EtaMetrics?,           // <-- Added for subsequent patient metrics
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
    val address: HospitalAddress?,
    val location: HospitalLocation?, // Added to map MongoDB GeoJSON location
    val departments: List<String>,
    val isActive: Boolean,
    val createdAt: String?,
    val updatedAt: String?
)

data class HospitalAddress(
    val street: String?,
    val city: String?,
    val state: String?,
    val zipCode: String?
)

data class HospitalLocation(
    val type: String?, // Usually "Point"
    val coordinates: List<Double>? // Array of numbers: [longitude, latitude]
)
data class QueueDashboardResponse(
    val success: Boolean = false,
    val data: DashboardData? = null
)
data class HospitalIdRequest(
    val hospitalId: String
)



@Parcelize
@Serializable
data class ResumeScanResponse(
    val success: Boolean,
    val message: String,
    val data: DoctorResumeData? = null
) : Parcelable

@Parcelize
@Serializable
data class DoctorResumeData(
    val name: String? = null,
    val email: String? = null,
    val phoneNumber: String? = null,
    val qualification: String? = null,
    val departments: List<String> = emptyList(),
    val specializations: List<String> = emptyList(),
    val experience: List<Experience> = emptyList(),
    val education: List<Education> = emptyList(),
    val rating: Double = 5.0,
    val matchScore: Double? = 4.8,
    val matchSummary: String? = "Exceptional match for current department needs based on 15 parameters.",
    val isAiVerified: Boolean = true,
    val availableDepartments: List<String>? = null
) : Parcelable

@Parcelize
@Serializable
data class Experience(
    val role: String,
    val hospital: String,
    val startDate: String? = null,
    val endDate: String? = null,
    val duration: String? = null,
    val highlights: List<String> = emptyList()
) : Parcelable

@Parcelize
@Serializable
data class Education(
    val degree: String,
    val institution: String,
    val year: String? = null,
    val status: String? = null,
    val type: String? = null
) : Parcelable



data class DashboardData(
    val activeQueue: List<ActiveQueueDto> = emptyList(),
    val recentHistory: List<HistoryItemDto> = emptyList()
)
data class RegisterDoctorRequest(
    val name: String?,
    val email: String?,
    val phoneNumber: String?,
    val password: String,
    val hospitalId: String,
    val departments: List<String>,
    val qualification: String?,
    val rating: Double?
)

data class RegisterDoctorResponse(
    val success: Boolean,
    val message: String,
    val data: DoctorRegistrationData?
)

data class DoctorRegistrationData(
    val name: String?,
    val email: String?,
    val phoneNumber: String?,
    val role: String?,
    val hospitalId: String?,
    val department: List<String>?,
    val doctorCode: String?,
    val qualification: String?,
    val rating: Double?
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
/*
data class SessionData(
    val sessionId: String,
    val queueStatus: String?, // "ACTIVE", "PAUSED", "CLOSED"
    val tokens: List<TokenItem>?
) */

data class SessionData(
    val sessionId: String,
    val queueStatus: String?, // "ACTIVE", "PAUSED", "CLOSED"
    val avgServiceTime: Int?, // <-- Added to capture active session service time
    val tokens: List<TokenItem>?
)

data class AdminDashboardResponse(
    val success: Boolean,
    val message: String,
    val data: AdminDashboardData
)

data class AdminDashboardData(
    val totalQueues: Int,
    val activeQueues: Int,
    val totalDoctors: Int,
    val doctorsWithQueues: Int,
    val totalWaitingPatients: Int
)


data class DoctorDirectoryResponse(
    val success: Boolean,
    val message: String,
    val data: List<DoctorDirectoryItem>
)

data class DoctorDirectoryItem(
    @SerializedName("_id") val id: String,
    val name: String,
    val email: String?,
    val specialization: String,
    val doctorCode: String,
    val role: String,
    val hospitalId: String,
    val status: String,
    val activeQueue: QueueData?
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
    val amount: Int,
    val notes:String
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

data class HospitalUpdateRequest(
    val hospitalId: String,
    val hospitalName: String,
    val description: String,
    val departments: List<String>,
    val phone: String,
    val address: HospitalAddress?,
    val bannerImageUrl: String?
)

data class StandardResponse(
    val success: Boolean,
    val message: String,
    val data: Any? = null
)
data class GlobalQueueResponseWrapper(
    val success: Boolean,
    val data: List<GlobalQueueItemRemote>
)

data class GlobalQueueItemRemote(
    val departmentId: String,
    val departmentName: String,
    val location: String,
    val waitingCount: Int,
    val avgWaitTime: String,
    val loadStatus: String,
    val assignedDoctorsCount: Int
)
data class GlobalQueueItem(
    val departmentId: String,
    val departmentName: String,
    val location: String,
    val waitingCount: Int,
    val avgWaitTime: String,
    val loadStatus: String, // e.g., "HIGH_LOAD", "NORMAL", "MODERATE"
    val assignedDoctorsCount: Int
)
data class QueueData(
    val _id: String,
    val hospitalId: String,
    val department: String,
    val doctorCode: String,
    val date: String,
    val tokens: List<Any>,
    val queueStatus:String
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



data class HospitalDetailItem(
    @SerializedName("_id") val id: String,
    val name: String,
    val code: String,
    val email: String,
    val address: HospitalAddress?, // Changed from String to HospitalAddress object
    val description: String?,
    val contactNumber: String,
    val departments: List<String>?,
    val distance: String?,
    val rating: Double?,
    val reviewsCount: Int?,
    val waitTime: String?,
    val imageUrl: String?
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
    val description: String?,
    val contactNumber: String?,
    val specialty: String,
    val imageUrl: String?,
    val consultationFee: Double,
    val peopleAhead: Int,
    val avgServiceTime: String?, // Added property to match backend response
    val estimatedWaitTime: String,
    @SerializedName("isJoined") val isJoined: Boolean = false,
    @SerializedName("isQueuePaused") val isQueuePaused: Boolean = false
)
data class EtaMetrics(
    val eta: Int,
    @SerializedName("queue_status") val queueStatus: String?,
    val avgServiceTime: Int,
    val activeCount: Int,
    val totalPeople: Int,
    val peopleAhead: Int,
    val waitingAhead: Int,
    val remaining: Double,
    val progress: Int,
    val currentMember: QueueTokenItem?,
    val servingMember: QueueTokenItem?
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
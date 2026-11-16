package com.example.construct.api

import com.google.gson.annotations.SerializedName

// ---------- AUTH ----------
data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val token: String,
    val user: UserInfo
)

data class UserInfo(
    val id: Int,
    val name: String,
    val email: String,
    val role: String,
    val employeeId: String?,
    val department: String?,
    val position: String?
)

// ---------- EMPLOYEE ----------
data class EmployeeDto(
    @SerializedName("employeeId")
    val employee_ID: Int,

    @SerializedName("firstName")
    val first_Name: String,

    @SerializedName("lastName")
    val last_Name: String,

    @SerializedName("idNumber")
    val id_Number: String,

    @SerializedName("phoneNumber")
    val phone_Number: String?,

    @SerializedName("position")
    val position: String?,

    @SerializedName("department")
    val department: String?,

    @SerializedName("dateHired")
    val date_Hired: String?,

    @SerializedName("qrCodeValue")
    val qrCode_Value: String?,

    @SerializedName("isActive")
    val isActive: Boolean?
)

data class LeaveRequestDto(
    val leave_ID: Int = 0,
    @SerializedName("employee_ID") val employee_ID: Int,
    val employeeName: String? = null,
    val type: String,
    val status: String = "Pending",
    val startDate: String,
    val endDate: String,
    val reason: String?,
    val submittedDate: String? = null
)
// ---------- ATTENDANCE ----------

data class AttendanceDto(
    @SerializedName("attendanceId")
    val attendance_ID: Int,

    @SerializedName("employeeId")
    val employee_ID: Int,

    @SerializedName("date")
    val date: String,

    @SerializedName("checkInTime")
    val checkInTime: String?,

    @SerializedName("checkOutTime")
    val checkOutTime: String?,

    @SerializedName("status")
    val status: String?
)


data class ClockRequest(val employeeId: Int)

// ---------- NOTIFICATION ----------
data class NotificationDto(
    val notification_ID: Int,
    val user_ID: Int?,
    val title: String,
    val message: String,
    val type: String?,
    val dateSent: String?,
    val isRead: Boolean?
)


data class LoginRecordDto(
    val loginRecord_ID: Int,
    val user_ID: Int,
    val name: String,
    val surname: String?,
    val initials: String?,
    val loginTime: String?,
    val location: String?
)

data class RegisterRequest(
    val fullName: String,
    val username: String,
    val email: String,
    val password: String,
    val role: String,
    val department: String
)

data class BroadcastRequest(
    val message: String
)

data class ProductivityDto(
    @SerializedName("productivity_ID") val productivity_ID: Int = 0, // Forces the correct mapping
    @SerializedName("supervisor_ID") val supervisor_ID: Int,
    val date: String,
    val workActivity: String,
    val targetQuantity: Double,
    val unit: String,
    val actualQuantity: Double?,
    val status: String
)

data class ProductivityResponse(
    val today: ProductivityDto?,
    val history: List<ProductivityDto>
)

data class SetTargetRequest(
    @SerializedName("supervisor_ID") val supervisor_ID: Int,
    val workActivity: String,
    val targetQuantity: Double,
    val unit: String
)

data class CompleteProductivityRequest(
    val actualQuantity: Double
)
data class UpdateLeaveStatusRequest(val status: String)

data class UpdateProfileRequest(
    val firstName: String,
    val lastName: String
)

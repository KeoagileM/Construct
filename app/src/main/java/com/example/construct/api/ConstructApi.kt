package com.example.construct.api

import retrofit2.http.*
import retrofit2.Response

interface ConstructApi {

    // ---------- AUTH ----------
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
    // ---------- EMPLOYEES ----------
    @GET("api/employees")
    suspend fun getEmployees(): List<EmployeeDto>

    @GET("api/employees/{id}")
    suspend fun getEmployee(@Path("id") id: Int): EmployeeDto
    // ---------- LEAVE ----------
    @GET("api/leaves/employee/{employeeId}")
    suspend fun getLeavesForEmployee(@Path("employeeId") employeeId: Int): Response<List<LeaveRequestDto>>
    @POST("api/leaves")
    suspend fun submitLeave(@Body leave: LeaveRequestDto): Response<LeaveRequestDto>
    @DELETE("api/leaves/{id}")
    suspend fun deleteLeave(@Path("id") id: Int)

    // ---------- ATTENDANCE ----------

    @GET("api/attendance/employee/{employeeId}")
    suspend fun getAttendanceForEmployee(@Path("employeeId") employeeId: Int): Response<List<AttendanceDto>>

    @POST("api/attendance/clockin")
    suspend fun clockIn(@Body request: ClockRequest): AttendanceDto

    @POST("api/attendance/clockout")
    suspend fun clockOut(@Body request: ClockRequest): AttendanceDto

    // ---------- NOTIFICATIONS ----------
    @GET("api/notifications/user/{userId}")
    suspend fun getNotifications(@Path("userId") userId: Int): Response<List<NotificationDto>>

    @PUT("api/users/{id}/profile")
    suspend fun updateProfile(
        @Path("id") id: Int,
        @Body request: UpdateProfileRequest
    ): Response<Unit>

    @PUT("api/notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: Int): Response<NotificationDto>
    @GET("api/loginrecords/user/{userId}")
    suspend fun getLoginRecords(@Path("userId") userId: Int): List<LoginRecordDto>

    @POST("api/notifications/broadcast")
    suspend fun sendBroadcast(@Body request: BroadcastRequest): Response<Unit>

    @GET("api/notifications/system")
    suspend fun getSystemNotifications(): Response<List<NotificationDto>>
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): retrofit2.Response<Unit>

    // ---------- SUPERVISOR LEAVE APPROVALS ----------
    @GET("api/leaves/pending")
    suspend fun getPendingLeaves(): Response<List<LeaveRequestDto>>
    @PUT("api/leaves/{id}/status")
    suspend fun updateLeaveStatus(
        @Path("id") id: Int,
        @Body request: UpdateLeaveStatusRequest
    ): Response<Unit>

    @GET("api/productivity/supervisor/{supervisorId}")
    suspend fun getSupervisorProductivity(@Path("supervisorId") supervisorId: Int): Response<List<ProductivityDto>>
    @POST("api/productivity/target")
    suspend fun setProductivityTarget(@Body request: SetTargetRequest): Response<ProductivityDto>

    @PUT("api/productivity/{id}/complete")
    suspend fun completeProductivity(
        @Path("id") id: Int,
        @Body request: CompleteProductivityRequest
    ): Response<ProductivityDto>
}
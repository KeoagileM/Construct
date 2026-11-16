package com.example.construct

data class User (

    val id: String,
    val name: String,
    val email: String,
    val role: UserRole,
    val employeeId: String,
    val department: String
)
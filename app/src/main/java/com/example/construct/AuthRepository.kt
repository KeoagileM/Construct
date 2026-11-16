package com.example.construct

interface AuthRepository {
    suspend fun login(email: String, pass: String): User?
    suspend fun register(
        name: String, email: String, pass: String,
        role: UserRole, empId: String, dept: String
    ): Boolean
    fun getCurrentUser(): User?
}
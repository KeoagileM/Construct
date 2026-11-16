package com.example.construct

import com.example.construct.api.LoginRequest
import com.example.construct.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

class RemoteAuthRepository(private val session: SessionManager) : AuthRepository {

    override suspend fun login(email: String, pass: String): User? {
        return try {
            val response = withContext(Dispatchers.IO) {
                RetrofitClient.api.login(LoginRequest(email, pass))
            }

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    session.saveSession(body.token, body.user)
                    return User(
                        id = body.user.id.toString(),
                        name = body.user.name,
                        email = body.user.email,
                        role = UserRole.valueOf(body.user.role.uppercase()),
                        employeeId = body.user.employeeId ?: "",
                        department = body.user.department ?: ""
                    )
                }
            } else if (response.code() == 403) {
                // This is our "Pending Approval" catch
                // We throw an exception with the custom message so the UI can show it
                throw Exception("PENDING_APPROVAL")
            }

            null
        } catch (e: Exception) {
            throw e // Re-throw so the Login screen can handle it
        }
    }

    override suspend fun register(
        name: String, email: String, pass: String,
        role: UserRole, empId: String, dept: String
    ): Boolean {
        // Not implemented yet — return false
        return false
    }

    override fun getCurrentUser(): User? {
        val token = session.getToken() ?: return null
        return User(
            id = session.getUserId().toString(),
            name = session.getUserName(),
            email = session.getEmail(),
            role = UserRole.valueOf(session.getRole().uppercase()),
            employeeId = session.getEmployeeId().toString(),
            department = ""
        )
    }
}
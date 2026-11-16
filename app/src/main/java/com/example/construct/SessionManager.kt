package com.example.construct

import android.content.Context
import com.example.construct.api.RetrofitClient
import com.example.construct.api.UserInfo

class SessionManager(context: Context) {

    private val prefs = context.getSharedPreferences("construct360", Context.MODE_PRIVATE)

    fun saveSession(token: String, user: UserInfo) {
        prefs.edit()
            .putString("token", token)
            .putInt("userId", user.id)
            .putString("name", user.name)
            .putString("email", user.email)
            .putString("role", user.role)
            .putString("employeeId", user.employeeId ?: "") // Saved as String e.g., "EMP-2026-005"
            .putString("department", user.department ?: "")
            .putString("position", user.position ?: "")
            .apply()

        // Also update the interceptor
        RetrofitClient.authToken = token
    }

    fun getToken(): String? = prefs.getString("token", null)
    fun getUserId(): Int = prefs.getInt("userId", 0)
    fun getUserName(): String = prefs.getString("name", "") ?: ""
    fun getEmail(): String = prefs.getString("email", "") ?: ""
    fun getRole(): String = prefs.getString("role", "Employee") ?: "Employee"

    // CHANGED: Now returns the full String (e.g., "EMP-2026-005")
    fun getEmployeeId(): String = prefs.getString("employeeId", "") ?: ""

    fun isLoggedIn(): Boolean = getToken() != null

    fun clear() {
        prefs.edit().clear().apply()
        RetrofitClient.authToken = null
    }

    // Restore token into the interceptor on app start
    fun restoreToken() {
        RetrofitClient.authToken = getToken()
    }

    fun getDepartment(): String {
        return prefs.getString("department", "") ?: ""
    }

    fun getPosition(): String {
        return prefs.getString("position", "") ?: ""
    }

    fun updateName(newName: String) {
        prefs.edit().putString("name", newName).apply()
    }
}
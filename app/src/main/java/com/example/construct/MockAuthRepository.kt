package com.example.construct

object MockAuthRepository: AuthRepository {

    // Pre-populated mock users
    private val users = mutableListOf(
        User("1", "Kopano Leshope", "kopano@rocla.com", UserRole.EMPLOYEE, "EMP-101", "Manufacturing"),
        User("2", "Sarah Jenkins", "supervisor@rocla.com", UserRole.SUPERVISOR, "SUP-201", "Site Management"),
    )

    private var currentUser: User? = null

    override suspend fun login(email: String, pass: String): User? {
        val user = users.find { it.email.equals(email, ignoreCase = true) }
        if (user != null) {
            currentUser = user
        }
        return user
    }

    override suspend fun register(
        name: String,
        email: String,
        pass: String,
        role: UserRole,
        empId: String,
        dept: String
    ): Boolean {
        if (users.any { it.email.equals(email, ignoreCase = true) }) return false
        val newUser = User((users.size + 1).toString(), name, email, role, empId, dept)
        users.add(newUser)
        currentUser = newUser
        return true
    }

    override fun getCurrentUser(): User? = currentUser
}
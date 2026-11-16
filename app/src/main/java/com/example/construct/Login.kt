package com.example.construct

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

class Login : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText

    private lateinit var btnSignIn: MaterialButton
    private lateinit var btnSsoGoogle: MaterialButton
    private lateinit var tvRegister: TextView

    private lateinit var session: SessionManager
    private lateinit var authRepo: AuthRepository



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnSignIn = findViewById(R.id.btnSignIn)
        btnSsoGoogle = findViewById(R.id.btnSsoGoogle)
        tvRegister = findViewById(R.id.tvRegister)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tvRegister.setOnClickListener {
            startActivity(Intent(this, Register::class.java))
        }

        btnSsoGoogle.setOnClickListener {
            Toast.makeText(this, "Google Single Sign-On selected", Toast.LENGTH_SHORT).show()

        }
        session = SessionManager(this)
        session.restoreToken()
        authRepo = RemoteAuthRepository(session)

        // Auto-skip login if already signed in
        if (session.isLoggedIn()) {
            val role = UserRole.valueOf(session.getRole().uppercase())
            navigateBasedOnRole(
                User(
                    id = session.getUserId().toString(),
                    name = session.getUserName(),
                    email = session.getEmail(),
                    role = role,
                    employeeId = session.getEmployeeId().toString(),
                    department = ""
                )
            )
            return
        }



        btnSignIn.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val pass = etPassword.text.toString().trim()

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSignIn.isEnabled = false
            btnSignIn.text = "Signing in..."

            lifecycleScope.launch {
                try {
                    val user = authRepo.login(email, pass)

                    if (user != null) {
                        navigateBasedOnRole(user)
                    } else {
                        Toast.makeText(this@Login, "Invalid credentials", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    if (e.message == "PENDING_APPROVAL") {
                        // Show a helpful alert dialog
                        androidx.appcompat.app.AlertDialog.Builder(this@Login)
                            .setTitle("Account Pending")
                            .setMessage("Your account has been created, but it requires administrator approval before you can log in.")
                            .setPositiveButton("OK", null)
                            .show()
                    } else {
                        Toast.makeText(this@Login, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                } finally {
                    btnSignIn.isEnabled = true
                    btnSignIn.text = "Sign In"
                }
            }
        }

        ProfilePreferences.applySavedTheme(this)
    }

    private fun navigateBasedOnRole(user: User) {
        Toast.makeText(this, "Welcome ${user.name}", Toast.LENGTH_SHORT).show()

        val target = when (user.role) {
            UserRole.EMPLOYEE -> EmployeeDashboard::class.java
            UserRole.SUPERVISOR -> SupervisorDashboard::class.java
        }
        startActivity(Intent(this, target))
        finish()
    }
}
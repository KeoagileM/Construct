package com.example.construct

import android.os.Bundle
import android.content.Intent
import android.widget.Toast
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class Login : AppCompatActivity() {


    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnDemoEmployee: MaterialCardView
    private lateinit var btnDemoSupervisor: MaterialCardView
    private lateinit var btnSignIn: MaterialButton

    private val authRepo: AuthRepository = MockAuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        // Initialize UI components using findViewById
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnDemoEmployee = findViewById(R.id.btnDemoEmployee)
        btnDemoSupervisor = findViewById(R.id.btnDemoSupervisor)
        btnSignIn = findViewById(R.id.btnSignIn)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }

        // Demo autofill listeners
        btnDemoEmployee.setOnClickListener {
            etEmail.setText("kopano@rocla.com")
            etPassword.setText("pass123")
        }

        btnDemoSupervisor.setOnClickListener {
            etEmail.setText("supervisor@rocla.com")
            etPassword.setText("pass123")
        }

        // Login Submit
        btnSignIn.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val pass = etPassword.text.toString().trim()

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(
                    this,
                    "Please fill in all fields",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val user = authRepo.login(email, pass)

            if (user != null) {
                navigateBasedOnRole(user)
            } else {
                Toast.makeText(
                    this,
                    "Invalid credentials",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun navigateBasedOnRole(user: User) {
        Toast.makeText(
            this,
            "Welcome ${user.name} (${user.role.name})",
            Toast.LENGTH_SHORT
        ).show()

        when (user.role) {
            UserRole.EMPLOYEE -> {
                val intent = Intent(this, EmployeeDashboard::class.java)
                startActivity(intent)
                finish()
            }

            UserRole.SUPERVISOR -> {
                // Add supervisor navigation here
            }
        }
    }


}

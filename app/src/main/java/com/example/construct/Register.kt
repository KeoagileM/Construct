package com.example.construct

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import com.example.construct.api.RegisterRequest
import com.example.construct.api.RetrofitClient
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class Register : AppCompatActivity() {

    private lateinit var etFullName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var spinnerDepartment: Spinner
    private lateinit var btnCreateAccount: MaterialButton

    private lateinit var cardRoleEmployee: MaterialCardView
    private lateinit var cardRoleSupervisor: MaterialCardView
    private lateinit var imgRoleEmployee: ImageView
    private lateinit var imgRoleSupervisor: ImageView
    private lateinit var tvSignIn: TextView

    private var selectedRole = "Employee"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        // Initialize Views
        etFullName = findViewById(R.id.etFullName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        tilEmail = findViewById(R.id.tilEmail)
        tilPassword = findViewById(R.id.tilPassword)
        spinnerDepartment = findViewById(R.id.spinnerDepartment)
        btnCreateAccount = findViewById(R.id.btnCreateAccount)
        tvSignIn = findViewById(R.id.tvSignIn)
        cardRoleEmployee = findViewById(R.id.cardRoleEmployee)
        cardRoleSupervisor = findViewById(R.id.cardRoleSupervisor)
        imgRoleEmployee = findViewById(R.id.imgRoleEmployee)
        imgRoleSupervisor = findViewById(R.id.imgRoleSupervisor)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Clear the errors as soon as the user starts typing again
        etEmail.doOnTextChanged { _, _, _, _ -> tilEmail.error = null }
        etPassword.doOnTextChanged { _, _, _, _ -> tilPassword.error = null }

        tvSignIn.setOnClickListener {
            startActivity(Intent(this, Login::class.java))
            finish()
        }

        setupDepartmentSpinner()
        setupRoleSelection()

        btnCreateAccount.setOnClickListener {
            performRegistration()
        }
    }

    private fun setupDepartmentSpinner() {
        val departments = listOf(
            "Select Department",
            "Sanitation",
            "Site Operations",
            "Structural Engineering"
        )

        val adapter = object : ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_item,
            departments
        ) {
            override fun isEnabled(position: Int): Boolean {
                return position != 0
            }

            override fun getDropDownView(
                position: Int,
                convertView: android.view.View?,
                parent: android.view.ViewGroup
            ): android.view.View {
                val view = super.getDropDownView(position, convertView, parent) as TextView
                if (position == 0) {
                    view.setTextColor(Color.GRAY)
                } else {
                    view.setTextColor(Color.parseColor("#0F172A"))
                }
                return view
            }
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDepartment.adapter = adapter
    }

    private fun setupRoleSelection() {
        fun updateRoleUI(isEmployeeSelected: Boolean) {
            if (isEmployeeSelected) {
                selectedRole = "Employee"
                cardRoleEmployee.setCardBackgroundColor(Color.parseColor("#EFF6FF"))
                cardRoleEmployee.strokeColor = Color.parseColor("#1D4ED8")
                cardRoleEmployee.strokeWidth = 2
                imgRoleEmployee.setColorFilter(Color.parseColor("#1D4ED8"))

                cardRoleSupervisor.setCardBackgroundColor(Color.parseColor("#FFFFFF"))
                cardRoleSupervisor.strokeColor = Color.parseColor("#CBD5E1")
                cardRoleSupervisor.strokeWidth = 1
                imgRoleSupervisor.setColorFilter(Color.parseColor("#64748B"))
            } else {
                selectedRole = "Supervisor"
                cardRoleSupervisor.setCardBackgroundColor(Color.parseColor("#EFF6FF"))
                cardRoleSupervisor.strokeColor = Color.parseColor("#1D4ED8")
                cardRoleSupervisor.strokeWidth = 2
                imgRoleSupervisor.setColorFilter(Color.parseColor("#1D4ED8"))

                cardRoleEmployee.setCardBackgroundColor(Color.parseColor("#FFFFFF"))
                cardRoleEmployee.strokeColor = Color.parseColor("#CBD5E1")
                cardRoleEmployee.strokeWidth = 1
                imgRoleEmployee.setColorFilter(Color.parseColor("#64748B"))
            }
        }

        cardRoleEmployee.setOnClickListener { updateRoleUI(true) }
        cardRoleSupervisor.setOnClickListener { updateRoleUI(false) }
    }

    /**
     * Returns an error message if the email is not a valid @gmail.com address,
     * or null when it passes.
     */
    private fun validateEmail(email: String): String? {
        val gmailPattern = Regex("^[A-Za-z0-9._%+-]+@gmail\\.com$", RegexOption.IGNORE_CASE)
        return if (gmailPattern.matches(email)) null
        else "Email must be a valid @gmail.com address"
    }

    /**
     * Returns an error message if the password is not strong enough,
     * or null when it passes all rules.
     */
    private fun validatePassword(password: String): String? {
        return when {
            password.length < 8 ->
                "Password must be at least 8 characters"
            !password.any { it.isDigit() } ->
                "Password must contain at least one number"
            !password.any { !it.isLetterOrDigit() && !it.isWhitespace() } ->
                "Password must contain at least one special character (e.g. ! @ # $ %)"
            else -> null
        }
    }

    private fun performRegistration() {
        val fullName = etFullName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val department = spinnerDepartment.selectedItem?.toString() ?: ""

        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Email rules (must be @gmail.com)
        val emailError = validateEmail(email)
        if (emailError != null) {
            tilEmail.error = emailError
            etEmail.requestFocus()
            return
        }

        // Password rules
        val passwordError = validatePassword(password)
        if (passwordError != null) {
            tilPassword.error = passwordError
            etPassword.requestFocus()
            return
        }

        if (spinnerDepartment.selectedItemPosition == 0) {
            Toast.makeText(this, "Please select a department", Toast.LENGTH_SHORT).show()
            return
        }

        val username = email.substringBefore("@")

        val request = RegisterRequest(
            fullName = fullName,
            username = username,
            email = email,
            password = password,
            role = selectedRole,
            department = department
        )

        btnCreateAccount.isEnabled = false
        btnCreateAccount.text = "Creating Account..."

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.register(request)
                if (response.isSuccessful) {
                    Toast.makeText(this@Register, "Account created! Pending Admin Approval.", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this@Register, Login::class.java))
                    finish()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(this@Register, "Error: ${errorBody ?: "Registration failed"}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@Register, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                btnCreateAccount.isEnabled = true
                btnCreateAccount.text = "Create Account"
            }
        }
    }
}
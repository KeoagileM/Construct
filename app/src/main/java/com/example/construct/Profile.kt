package com.example.construct

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.construct.api.RetrofitClient
import com.example.construct.api.UpdateProfileRequest
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class Profile : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var etFirstName: EditText
    private lateinit var etSurname: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)

        sessionManager = SessionManager(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 1. Load current data
        loadProfileDetails()

        // 2. Setup Buttons
        setupProfileLogout()
        setupProfileSettings()
        setupSaveChanges()
    }

    private fun loadProfileDetails() {
        val name = sessionManager.getUserName().trim()
        val email = sessionManager.getEmail()
        val role = sessionManager.getRole()
        val department = sessionManager.getDepartment()
        val position = sessionManager.getPosition()

        findViewById<TextView>(R.id.tvProfileAvatar).text = getInitials(name)
        findViewById<TextView>(R.id.tvProfileFullName).text = name.ifBlank { "User" }
        findViewById<TextView>(R.id.tvProfileRole).text = role.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }

        etFirstName = findViewById(R.id.etProfileFirstName)
        etSurname = findViewById(R.id.etProfileSurname)

        etFirstName.setText(name.split(Regex("\\s+")).firstOrNull().orEmpty())
        etSurname.setText(name.split(Regex("\\s+")).drop(1).joinToString(" "))

        findViewById<TextView>(R.id.tvProfileEmail).text = email.ifBlank { "Not available" }
        findViewById<TextView>(R.id.tvProfileDepartment).text = department.ifBlank { "Not available" }
        findViewById<TextView>(R.id.tvProfileRoleDetail).text = position.ifBlank { role.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() } }
        findViewById<TextView>(R.id.tvAppVersion).text = getAppVersion()
    }

    private fun setupSaveChanges() {
        val btnSaveProfile = findViewById<MaterialButton>(R.id.btnSaveProfile)

        btnSaveProfile.setOnClickListener {
            val newFirstName = etFirstName.text.toString().trim()
            val newLastName = etSurname.text.toString().trim()

            if (newFirstName.isEmpty() || newLastName.isEmpty()) {
                Toast.makeText(this, "First name and surname cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userId = sessionManager.getUserId()
            if (userId == 0) return@setOnClickListener

            btnSaveProfile.isEnabled = false
            btnSaveProfile.text = "Saving..."

            lifecycleScope.launch {
                try {
                    val request = UpdateProfileRequest(firstName = newFirstName, lastName = newLastName)
                    val response = RetrofitClient.api.updateProfile(userId, request)

                    if (response.isSuccessful) {
                        // Update the local session so the header and other screens update instantly
                        val fullName = "$newFirstName $newLastName".trim()
                        sessionManager.updateName(fullName)

                        Toast.makeText(this@Profile, "Profile updated successfully!", Toast.LENGTH_SHORT).show()

                        // Reload the UI to show the new initials
                        loadProfileDetails()
                        updateHeader()
                    } else {
                        Toast.makeText(this@Profile, "Failed to update: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@Profile, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    btnSaveProfile.isEnabled = true
                    btnSaveProfile.text = "Save Changes"
                }
            }
        }
    }

    // --- Add this function to SessionManager.kt ---
    // You will need to add `fun updateName(newName: String)` to your SessionManager class

    private fun getInitials(name: String): String {
        return name.split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .take(2)
            .map { it.first().uppercaseChar() }
            .joinToString("")
            .ifBlank { "?" }
    }

    private fun getAppVersion(): String {
        return try {
            val info = packageManager.getPackageInfo(packageName, 0)
            "Version ${info.versionName ?: "1.0"}"
        } catch (e: Exception) {
            "Version unavailable"
        }
    }

    private fun setupProfileLogout() {
        val btnProfileLogout = findViewById<MaterialButton>(R.id.btnProfileLogout)

        btnProfileLogout.setOnClickListener {
            SessionManager(this).clear()
            Toast.makeText(this, "You have been logged out", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, Login::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }

    private fun setupProfileSettings() {
        val themeSpinner = findViewById<Spinner>(R.id.spinnerTheme)
        val textSizeSpinner = findViewById<Spinner>(R.id.spinnerTextSize)

        val themes = resources.getStringArray(R.array.profile_theme_options)
        val textSizes = resources.getStringArray(R.array.profile_text_size_options)

        themeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, themes).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        textSizeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, textSizes).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        themeSpinner.setSelection(themes.indexOf(ProfilePreferences.getTheme(this)).coerceAtLeast(0), false)
        textSizeSpinner.setSelection(textSizes.indexOf(ProfilePreferences.getTextSize(this)).coerceAtLeast(0), false)

        themeSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val selectedTheme = themes[position]
                if (selectedTheme != ProfilePreferences.getTheme(this@Profile)) {
                    ProfilePreferences.saveTheme(this@Profile, selectedTheme)
                    Toast.makeText(this@Profile, "Theme updated", Toast.LENGTH_SHORT).show()
                }
            }
        }

        textSizeSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val selectedSize = textSizes[position]
                if (selectedSize != ProfilePreferences.getTextSize(this@Profile)) {
                    ProfilePreferences.saveTextSize(this@Profile, selectedSize)
                    Toast.makeText(this@Profile, "Text size saved. Reopen the screen to apply it.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateHeader()
    }
}
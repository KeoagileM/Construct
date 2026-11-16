package com.example.construct

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.construct.api.LeaveRequestDto
import com.example.construct.api.RetrofitClient
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class ApplyLeave : AppCompatActivity() {

    private lateinit var session: SessionManager
    private lateinit var etStartDate: TextInputEditText
    private lateinit var etEndDate: TextInputEditText
    private lateinit var actLeaveType: MaterialAutoCompleteTextView
    private lateinit var etReason: TextInputEditText
    private lateinit var btnSubmitLeave: MaterialButton

    private var startDateCalendar: Calendar? = null
    private var endDateCalendar: Calendar? = null

    // Format for displaying in the UI (e.g., 12 Oct 2026)
    private val displayFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    // Format for sending to C# API (ISO 8601 UTC) (e.g., 2026-10-12T00:00:00Z
    private val apiFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_apply_leave)

        session = SessionManager(this)

        // Initialize Views
        val navLeaveRequest = findViewById<LinearLayout>(R.id.navLeaveRequest)
        val navHome = findViewById<LinearLayout>(R.id.navHome)
        val navNotifications = findViewById<LinearLayout>(R.id.navNotifications)
        val navAttendance = findViewById<LinearLayout>(R.id.navAttendance)
        val tvHeaderAvatar = findViewById<TextView>(R.id.tvHeaderAvatar)
        val btnHeaderNotifications = findViewById<FrameLayout>(R.id.btnHeaderNotifications)

        btnSubmitLeave = findViewById(R.id.btnSubmitLeave)
        etStartDate = findViewById(R.id.etStartDate)
        etEndDate = findViewById(R.id.etEndDate)
        actLeaveType = findViewById(R.id.actLeaveType)
        etReason = findViewById(R.id.etReason)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Setup Navigation
        navAttendance.setOnClickListener { startActivity(Intent(this, Attendance::class.java)) }
        navNotifications.setOnClickListener { startActivity(Intent(this, Notifications::class.java)) }
        navHome.setOnClickListener { startActivity(Intent(this, EmployeeDashboard::class.java)) }
        navLeaveRequest.setOnClickListener { startActivity(Intent(this, Leaves::class.java)) }
        tvHeaderAvatar.setOnClickListener { startActivity(Intent(this, Profile::class.java)) }
        btnHeaderNotifications.setOnClickListener { startActivity(Intent(this, Notifications::class.java)) }

        setupLeaveTypeDropdown()
        setupDatePickers()

        btnSubmitLeave.setOnClickListener {
            submitLeaveRequest()
        }
    }

    private fun setupLeaveTypeDropdown() {
        val leaveTypes = arrayOf("Annual Leave", "Sick Leave", "Personal Leave")
        val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, leaveTypes)
        actLeaveType.setAdapter(adapter)
    }

    private fun setupDatePickers() {
        val calendar = Calendar.getInstance()

        etStartDate.setOnClickListener {
            DatePickerDialog(this, { _, year, month, day ->
                startDateCalendar = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, day)
                }
                etStartDate.setText(displayFormat.format(startDateCalendar!!.time))
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        etEndDate.setOnClickListener {
            DatePickerDialog(this, { _, year, month, day ->
                endDateCalendar = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, day)
                }
                etEndDate.setText(displayFormat.format(endDateCalendar!!.time))
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }
    }


    private fun submitLeaveRequest() {
        val leaveType = actLeaveType.text.toString().trim()
        val reason = etReason.text.toString().trim()

        if (leaveType.isEmpty() || startDateCalendar == null || endDateCalendar == null || reason.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (endDateCalendar!!.timeInMillis < startDateCalendar!!.timeInMillis) {
            Toast.makeText(this, "End date cannot be before start date", Toast.LENGTH_SHORT).show()
            return
        }

        // 1. Get the String ID from SessionManager (e.g., "EMP-2026-005")
        val employeeIdString = session.getEmployeeId()

        // 2. Extract the numeric ID safely (e.g., "5")
        val numericEmployeeId = employeeIdString.split("-").lastOrNull()?.toIntOrNull() ?: 0

        if (numericEmployeeId == 0) {
            Toast.makeText(this, "Error: Invalid Employee ID", Toast.LENGTH_SHORT).show()
            return
        }

        // 3. Create the request
        val request = LeaveRequestDto(
            employee_ID = numericEmployeeId, // Sends the integer 5 to the API
            type = leaveType,
            startDate = apiFormat.format(startDateCalendar!!.time),
            endDate = apiFormat.format(endDateCalendar!!.time),
            reason = reason
        )

        btnSubmitLeave.isEnabled = false
        btnSubmitLeave.text = "Submitting...".toString()

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.submitLeave(request)
                if (response.isSuccessful) {
                    Toast.makeText(this@ApplyLeave, "Leave request submitted!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@ApplyLeave, Leaves::class.java))
                    finish()
                } else {
                    // This is the important part: read the error body
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(this@ApplyLeave, "Failed: $errorBody", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ApplyLeave, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                btnSubmitLeave.isEnabled = true
                btnSubmitLeave.text = "Submit Leave Request".toString()
            }
        }
    }
}
package com.example.construct

import android.content.Intent
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.construct.api.RetrofitClient
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

class EmployeeDashboard : AppCompatActivity() {

    private lateinit var dots: List<ImageView>
    private lateinit var btnViewQr: MaterialButton
    private lateinit var session: SessionManager

    // UI Elements for Stats
    private lateinit var tvEmployeeName: TextView
    private lateinit var tvDaysPresent: TextView
    private lateinit var tvLeavesPending: TextView
    private lateinit var tvLeaveBalance: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_employee_dashboard)

        session = SessionManager(this)

        // Initialize Views
        val cardApplyLeave = findViewById<MaterialCardView>(R.id.cardApplyLeave)
        val cardProfile = findViewById<MaterialCardView>(R.id.cardProfile)
        val navLeaveRequest = findViewById<LinearLayout>(R.id.navLeaveRequest)
        val navHome = findViewById<LinearLayout>(R.id.navHome)
        val navNotifications = findViewById<LinearLayout>(R.id.navNotifications)
        val navAttendance = findViewById<LinearLayout>(R.id.navAttendance)
        val btnHeaderNotifications = findViewById<FrameLayout>(R.id.btnHeaderNotifications)
        val tvHeaderAvatar = findViewById<TextView>(R.id.tvHeaderAvatar)

        tvEmployeeName = findViewById(R.id.tvEmployeeName)
        tvDaysPresent = findViewById(R.id.tvDaysPresent)
        tvLeavesPending = findViewById(R.id.tvLeavesPending)
        tvLeaveBalance = findViewById(R.id.tvLeaveBalance)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize scroll indicator
        dots = listOf(
            findViewById(R.id.dot1),
            findViewById(R.id.dot2),
            findViewById(R.id.dot3),
            findViewById(R.id.dot4)
        )

        // Navigation Listeners
        navAttendance.setOnClickListener { startActivity(Intent(this, Attendance::class.java)) }
        navHome.setOnClickListener { /* Already here */ }
        navNotifications.setOnClickListener { startActivity(Intent(this, Notifications::class.java)) }
        btnHeaderNotifications.setOnClickListener { startActivity(Intent(this, Notifications::class.java)) }
        navLeaveRequest.setOnClickListener { startActivity(Intent(this, Leaves::class.java)) }

        cardApplyLeave.setOnClickListener { startActivity(Intent(this, ApplyLeave::class.java)) }
        cardProfile.setOnClickListener { startActivity(Intent(this, Profile::class.java)) }
        tvHeaderAvatar.setOnClickListener { startActivity(Intent(this, Profile::class.java)) }

        // QR Button
        btnViewQr = findViewById(R.id.btnViewQr)
        btnViewQr.setOnClickListener {
            val intent = Intent(this, EmployeeQR::class.java)
            startActivity(intent)
        }

        // Setup UI Data
        loadUserData(tvHeaderAvatar)
        loadDashboardStats()

        setupScrollIndicator()
        setupLogout()
    }

    private fun loadUserData(tvAvatar: TextView) {
        // 1. Set the Welcome Name
        val fullName = session.getUserName()
        tvEmployeeName.text = fullName

        // 2. Set the Header Avatar Initials (Fixes the crash)
        if (fullName.isNotBlank()) {
            val initials = fullName.split(" ")
                .filter { it.isNotBlank() }
                .take(2)
                .joinToString("") { it.first().uppercase() }
            tvAvatar.text = initials
        }
    }

    private fun loadDashboardStats() {
        val employeeIdString = session.getEmployeeId()
        val numericEmployeeId = employeeIdString.split("-").lastOrNull()?.toIntOrNull() ?: 0

        if (numericEmployeeId == 0) return

        lifecycleScope.launch {
            try {
                // 1. Fetch Leaves to calculate "Pending"
                val leavesResponse = RetrofitClient.api.getLeavesForEmployee(numericEmployeeId)
                val leaves = leavesResponse.body() ?: emptyList()

                val pendingCount = leaves.count { it.status.equals("Pending", ignoreCase = true) }
                tvLeavesPending.text = String.format("%02d", pendingCount)

                // 2. Fetch Attendance to calculate "Days Present"
                val attendanceResponse = RetrofitClient.api.getAttendanceForEmployee(numericEmployeeId)
                val attendance = attendanceResponse.body() ?: emptyList()

                val daysPresent = attendance.count { it.status.equals("Present", ignoreCase = true) }
                tvDaysPresent.text = String.format("%02d", daysPresent)

                // 3. Calculate Leave Balance (e.g., Total 15 days - Approved leaves)
                val approvedLeaves = leaves.filter { it.status.equals("Approved", ignoreCase = true) }
                // Rough estimate: 1 day per leave request
                val leaveBalance = 15 - approvedLeaves.size
                tvLeaveBalance.text = if (leaveBalance < 0) "0" else leaveBalance.toString()

            } catch (e: Exception) {
                // If API fails, show error in Logcat but don't crash the UI
                e.printStackTrace()
            }
        }
    }

    private fun setupLogout() {
        val btnLogout = findViewById<MaterialButton>(R.id.btnLogout)

        btnLogout.setOnClickListener {
            SessionManager(this).clear()
            Toast.makeText(this, "You have been logged out", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, Login::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }

    private fun setupScrollIndicator() {
        val statsScrollView = findViewById<android.widget.HorizontalScrollView>(R.id.statsScrollView)
        statsScrollView.setOnScrollChangeListener { _, scrollX, _, _, _ ->
            // Convert 152dp card width into pixels
            val cardWidthPx = (152 * resources.displayMetrics.density).toInt()
            val position = (scrollX + (cardWidthPx / 2)) / cardWidthPx
            val activeIndex = position.coerceIn(0, 3)
            dots.forEachIndexed { index, dot ->
                if (index == activeIndex) {
                    dot.setImageResource(R.drawable.ic_dot_active)
                } else {
                    dot.setImageResource(R.drawable.ic_dot_inactive)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateHeader()
    }
}
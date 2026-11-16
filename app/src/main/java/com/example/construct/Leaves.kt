package com.example.construct

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.construct.api.RetrofitClient
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class Leaves : AppCompatActivity() {

    private lateinit var session: SessionManager
    private lateinit var container: LinearLayout
    private lateinit var emptyState: LinearLayout
    private lateinit var templateCard: MaterialCardView
    private lateinit var sampleCard2: MaterialCardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_leaves)

        session = SessionManager(this)

        // Initialize Navigation & Buttons
        val navLeaveRequest = findViewById<LinearLayout>(R.id.navLeaveRequest)
        val fabAddLeave = findViewById<FloatingActionButton>(R.id.fabAddLeave)
        val navHome = findViewById<LinearLayout>(R.id.navHome)
        val navNotifications = findViewById<LinearLayout>(R.id.navNotifications)
        val navAttendance = findViewById<LinearLayout>(R.id.navAttendance)
        val btnHeaderNotifications = findViewById<FrameLayout>(R.id.btnHeaderNotifications)
        val tvHeaderAvatar = findViewById<TextView>(R.id.tvHeaderAvatar)

        // Initialize Containers
        container = findViewById(R.id.leaveRequestsContainer)
        emptyState = findViewById(R.id.emptyState)

        // Hide the hardcoded sample cards from the XML
        templateCard = findViewById(R.id.cardLeaveRequest1)
        sampleCard2 = findViewById(R.id.cardLeaveRequest2)
        templateCard.visibility = View.GONE
        sampleCard2.visibility = View.GONE

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Navigation Listeners
        navAttendance.setOnClickListener { startActivity(Intent(this, Attendance::class.java)) }
        navNotifications.setOnClickListener { startActivity(Intent(this, Notifications::class.java)) }
        navHome.setOnClickListener { startActivity(Intent(this, EmployeeDashboard::class.java)) }
        navLeaveRequest.setOnClickListener { startActivity(Intent(this, Leaves::class.java)) }
        tvHeaderAvatar.setOnClickListener { startActivity(Intent(this, Profile::class.java)) }
        btnHeaderNotifications.setOnClickListener { startActivity(Intent(this, Notifications::class.java)) }

        fabAddLeave.setOnClickListener { startActivity(Intent(this, ApplyLeave::class.java)) }
    }

    override fun onResume() {
        updateHeader()
        super.onResume()
        // Reload leaves every time the screen is opened (e.g., after submitting a new leave)
        loadLeaveRequests()
    }

    private fun loadLeaveRequests() {
        // 1. Get the String ID (e.g., "EMP-2026-005") and extract the number (5)
        val employeeIdString = session.getEmployeeId()
        val numericEmployeeId = employeeIdString.split("-").lastOrNull()?.toIntOrNull() ?: 0

        if (numericEmployeeId == 0) return

        lifecycleScope.launch {
            try {
                // 2. Fetch from API
                val response = RetrofitClient.api.getLeavesForEmployee(numericEmployeeId)
                val leaves = response.body() ?: emptyList()

                // 3. Clear old dynamic views (keep the title and empty state)
                val childCount = container.childCount
                for (i in childCount - 1 downTo 0) {
                    val view = container.getChildAt(i)
                    if (view != templateCard && view != sampleCard2 && view != emptyState && view !is TextView) {
                        container.removeViewAt(i)
                    }
                }

                // 4. Show data or empty state
                if (leaves.isEmpty()) {
                    emptyState.visibility = View.VISIBLE
                } else {
                    emptyState.visibility = View.GONE
                    for (leave in leaves) {
                        addLeaveCardToView(leave)
                    }
                }

            } catch (e: Exception) {
                Toast.makeText(this@Leaves, "Failed to load leaves: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Dynamically builds the Card UI using your existing design colors
    private fun addLeaveCardToView(leave: com.example.construct.api.LeaveRequestDto) {

        // 1. Main Card
        val card = MaterialCardView(this)
        val cardParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0, 0, 0, 32) }
        card.layoutParams = cardParams
        card.setCardBackgroundColor(Color.WHITE)
        card.radius = 36f
        card.cardElevation = 8f

        // 2. Inner Layout
        val mainLayout = LinearLayout(this)
        mainLayout.orientation = LinearLayout.VERTICAL
        mainLayout.setPadding(40, 40, 40, 40)
        card.addView(mainLayout)

        // 3. Top Row (Title + Status)
        val topRow = LinearLayout(this)
        topRow.orientation = LinearLayout.HORIZONTAL
        topRow.gravity = Gravity.CENTER_VERTICAL
        mainLayout.addView(topRow)

        val titleLayout = LinearLayout(this)
        titleLayout.orientation = LinearLayout.VERTICAL
        titleLayout.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        topRow.addView(titleLayout)

        val tvType = TextView(this)
        tvType.text = leave.type
        tvType.textSize = 17f
        tvType.setTextColor(Color.parseColor("#0F172A"))
        tvType.setTypeface(null, Typeface.BOLD)
        titleLayout.addView(tvType)

        val tvSubtitle = TextView(this)
        tvSubtitle.text = "Leave Request"
        tvSubtitle.textSize = 11f
        tvSubtitle.setTextColor(Color.parseColor("#64748B"))
        titleLayout.addView(tvSubtitle)

        // Status Badge
        val badgeCard = MaterialCardView(this)
        badgeCard.radius = 40f
        badgeCard.cardElevation = 0f
        badgeCard.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        val tvStatus = TextView(this)
        tvStatus.text = leave.status
        tvStatus.textSize = 11f
        tvStatus.setTypeface(null, Typeface.BOLD)
        tvStatus.setPadding(30, 15, 30, 15)

        if (leave.status.equals("Approved", true)) {
            badgeCard.setCardBackgroundColor(Color.parseColor("#DCFCE7"))
            tvStatus.setTextColor(Color.parseColor("#166534"))
        } else if (leave.status.equals("Rejected", true)) {
            badgeCard.setCardBackgroundColor(Color.parseColor("#FEE2E2"))
            tvStatus.setTextColor(Color.parseColor("#991B1B"))
        } else {
            // Default to Pending
            badgeCard.setCardBackgroundColor(Color.parseColor("#FEF3C7"))
            tvStatus.setTextColor(Color.parseColor("#92400E"))
        }
        badgeCard.addView(tvStatus)
        topRow.addView(badgeCard)

        // 4. Divider
        val divider = View(this)
        val divParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2)
        divParams.setMargins(0, 30, 0, 30)
        divider.layoutParams = divParams
        divider.setBackgroundColor(Color.parseColor("#E2E8F0"))
        mainLayout.addView(divider)

        // 5. Dates
        val tvDates = TextView(this)
        val displayFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val apiFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        try {
            val start = apiFormat.parse(leave.startDate.substringBefore("."))
            val end = apiFormat.parse(leave.endDate.substringBefore("."))
            tvDates.text = "${displayFormat.format(start!!)} - ${displayFormat.format(end!!)}"
        } catch (e: Exception) {
            tvDates.text = "${leave.startDate} - ${leave.endDate}"
        }

        tvDates.textSize = 14f
        tvDates.setTextColor(Color.parseColor("#0F172A"))
        tvDates.setTypeface(null, Typeface.BOLD)
        mainLayout.addView(tvDates)

        // 6. Reason
        val tvReason = TextView(this)
        tvReason.text = leave.reason ?: "No reason provided"
        tvReason.textSize = 14f
        tvReason.setTextColor(Color.parseColor("#64748B"))
        tvReason.setPadding(0, 15, 0, 0)
        mainLayout.addView(tvReason)

        // Add the completed card to the screen
        container.addView(card)
    }
}
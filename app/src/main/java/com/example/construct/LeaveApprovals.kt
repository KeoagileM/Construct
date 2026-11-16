package com.example.construct

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.construct.api.RetrofitClient
import com.example.construct.api.UpdateLeaveStatusRequest
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class LeaveApprovals : AppCompatActivity() {

    private var container: LinearLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_leave_approvals)

        // 1. Setup Navigation
        val navHome = findViewById<LinearLayout>(R.id.navHome)
        val navSendNotification = findViewById<LinearLayout>(R.id.navSendNotification)
        val navLeaveApprovals = findViewById<LinearLayout>(R.id.navLeaveApprovals)
        val navProductivity = findViewById<LinearLayout>(R.id.navProductivity)
        val tvHeaderAvatar = findViewById<TextView>(R.id.tvHeaderAvatar)

        navHome.setOnClickListener { startActivity(Intent(this, SupervisorDashboard::class.java)) }
        navSendNotification.setOnClickListener { startActivity(Intent(this, SendNotification::class.java)) }
        navProductivity.setOnClickListener { startActivity(Intent(this, Productivity::class.java)) }
        tvHeaderAvatar.setOnClickListener { startActivity(Intent(this, Profile::class.java)) }
        navLeaveApprovals.setOnClickListener { startActivity(Intent(this, LeaveApprovals::class.java)) }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 2. Safely find the container and hide the mock card
        val rootView = findViewById<View>(android.R.id.content)
        val mockCard = findMockCard(rootView)

        if (mockCard != null) {
            mockCard.visibility = View.GONE // Hide the mockup
            container = mockCard.parent as? LinearLayout // The container is the parent of the mock card
        } else {
            Toast.makeText(this, "Error: Could not find layout container", Toast.LENGTH_LONG).show()
        }

        // 3. Load data
        if (container != null) {
            loadPendingLeaves()
        }
    }

    // Helper to find the mock card anywhere in the view hierarchy
    private fun findMockCard(view: View): MaterialCardView? {
        if (view is MaterialCardView) {
            if (view.findViewById<MaterialButton>(R.id.btnApprove) != null) {
                return view
            }
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val result = findMockCard(view.getChildAt(i))
                if (result != null) return result
            }
        }
        return null
    }

    override fun onResume() {
        super.onResume()
        updateHeader()
    }

    private fun loadPendingLeaves() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.getPendingLeaves()

                if (!response.isSuccessful) {
                    Toast.makeText(this@LeaveApprovals, "API Error: ${response.code()}", Toast.LENGTH_LONG).show()
                    return@launch
                }

                val pendingLeaves = response.body() ?: emptyList()

                if (pendingLeaves.isEmpty()) {
                    Toast.makeText(this@LeaveApprovals, "No pending leaves right now.", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                for (leave in pendingLeaves) {
                    val card = createDynamicCard(leave)
                    container?.addView(card)
                }

            } catch (e: Exception) {
                Toast.makeText(this@LeaveApprovals, "Network Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun createDynamicCard(leave: com.example.construct.api.LeaveRequestDto): MaterialCardView {
        val card = MaterialCardView(this)
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0, 40, 0, 0) }
        card.layoutParams = params
        card.radius = 24f
        card.cardElevation = 2f
        card.setStrokeColor(Color.parseColor("#E2E8F0"))
        card.setCardBackgroundColor((Color.parseColor("#FFFFFF")))
        card.strokeWidth = 2

        val inner = LinearLayout(this)
        inner.orientation = LinearLayout.VERTICAL
        inner.setPadding(40, 40, 40, 40)
        card.addView(inner)

        // Top Row (Name + Pending badge)
        val topRow = RelativeLayout(this)
        topRow.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        val tvName = TextView(this)
        // Pull the name from the API response!
        tvName.text = leave.employeeName ?: "Employee #${leave.employee_ID}"
        tvName.textSize = 16f
        tvName.setTypeface(null, Typeface.BOLD)
        tvName.setTextColor(Color.parseColor("#0F172A"))
        topRow.addView(tvName)

        val tvBadge = TextView(this)
        tvBadge.text = "PENDING"
        tvBadge.textSize = 10f
        tvBadge.setTypeface(null, Typeface.BOLD)
        tvBadge.setTextColor(Color.parseColor("#854D0E"))
        tvBadge.setBackgroundColor(Color.parseColor("#FEF9C3"))
        tvBadge.setPadding(20, 5, 20, 5)
        val badgeParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        ).apply { addRule(RelativeLayout.ALIGN_PARENT_END) }
        tvBadge.layoutParams = badgeParams
        topRow.addView(tvBadge)
        inner.addView(topRow)

        // Leave Type
        val tvType = TextView(this)
        tvType.text = "${leave.type} • Request"
        tvType.textSize = 12f
        tvType.setTypeface(null, Typeface.BOLD)
        tvType.setTextColor(Color.parseColor("#2563EB"))
        tvType.setPadding(0, 15, 0, 0)
        inner.addView(tvType)

        // Dates
        val tvDates = TextView(this)
        var dateText = "${leave.startDate} - ${leave.endDate}"
        try {
            // Parse the C# ISO format string
            val apiFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            val displayFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val start = apiFormat.parse(leave.startDate.substringBefore("."))
            val end = apiFormat.parse(leave.endDate.substringBefore("."))
            if (start != null && end != null) {
                dateText = "${displayFormat.format(start)} - ${displayFormat.format(end)}"
            }
        } catch (e: Exception) {}

        tvDates.text = "Dates: $dateText"
        tvDates.textSize = 12f
        tvDates.setTextColor(Color.parseColor("#64748B"))
        tvDates.setPadding(0, 10, 0, 0)
        inner.addView(tvDates)

        // Reason
        val tvReason = TextView(this)
        tvReason.text = "Reason: ${leave.reason ?: "None provided"}"
        tvReason.textSize = 13f
        tvReason.setTextColor(Color.parseColor("#0F172A"))
        tvReason.setPadding(0, 10, 0, 20)
        inner.addView(tvReason)

        // Action Buttons
        val btnRow = LinearLayout(this)
        btnRow.orientation = LinearLayout.HORIZONTAL
        btnRow.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        val btnReject = MaterialButton(this)
        btnReject.text = "Reject"
        btnReject.setTextColor(Color.parseColor("#991B1B"))
        btnReject.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.TRANSPARENT)
        btnReject.strokeColor = android.content.res.ColorStateList.valueOf(Color.parseColor("#991B1B"))
        btnReject.strokeWidth = 2
        btnReject.cornerRadius = 16
        val rejectParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(0, 0, 10, 0) }
        btnReject.layoutParams = rejectParams

        val btnApprove = MaterialButton(this)
        btnApprove.text = "Approve"
        btnApprove.setTextColor(Color.WHITE)
        btnApprove.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#166534"))
        btnApprove.cornerRadius = 16
        val approveParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(10, 0, 0, 0) }
        btnApprove.layoutParams = approveParams

        btnRow.addView(btnReject)
        btnRow.addView(btnApprove)
        inner.addView(btnRow)

        btnApprove.setOnClickListener { updateLeaveStatus(leave.leave_ID, "Approved", card) }
        btnReject.setOnClickListener { updateLeaveStatus(leave.leave_ID, "Rejected", card) }

        return card
    }

    private fun updateLeaveStatus(leaveId: Int, newStatus: String, card: MaterialCardView) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.updateLeaveStatus(leaveId, UpdateLeaveStatusRequest(newStatus))
                if (response.isSuccessful) {
                    Toast.makeText(this@LeaveApprovals, "Leave $newStatus!", Toast.LENGTH_SHORT).show()
                    card.visibility = View.GONE // Remove from UI immediately
                } else {
                    Toast.makeText(this@LeaveApprovals, "Failed to update", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@LeaveApprovals, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
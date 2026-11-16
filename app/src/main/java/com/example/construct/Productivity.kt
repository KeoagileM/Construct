package com.example.construct

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.construct.api.*
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class Productivity : AppCompatActivity() {

    private lateinit var session: SessionManager

    // UI Elements
    private lateinit var cardSetTarget: MaterialCardView
    private lateinit var historyContainer: LinearLayout
    private lateinit var tvEmptyHistory: TextView

    // Inputs
    private lateinit var etWorkActivity: EditText
    private lateinit var etTargetQuantity: EditText
    private lateinit var spUnit: Spinner
    private lateinit var btnSetTarget: MaterialButton

    // Today's Display
    private lateinit var tvTodayDate: TextView
    private lateinit var tvTodayStatus: TextView
    private lateinit var tvTodayActivity: TextView
    private lateinit var tvTodayTarget: TextView
    private lateinit var tvTodayActual: TextView
    private lateinit var etActualQuantity: EditText
    private lateinit var btnCompleteProductivity: MaterialButton

    private var currentTodayRecord: ProductivityDto? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_productivity)

        session = SessionManager(this)

        // Setup Nav
        val navHome = findViewById<LinearLayout>(R.id.navHome)
        val navSendNotification = findViewById<LinearLayout>(R.id.navSendNotification)
        val navLeaveApprovals = findViewById<LinearLayout>(R.id.navLeaveApprovals)
        val navProductivity = findViewById<LinearLayout>(R.id.navProductivity)
        val btnHeaderNotifications = findViewById<FrameLayout>(R.id.btnHeaderNotifications)
        val tvHeaderAvatar = findViewById<TextView>(R.id.tvHeaderAvatar)

        navHome.setOnClickListener { startActivity(Intent(this, SupervisorDashboard::class.java)) }
        navSendNotification.setOnClickListener { startActivity(Intent(this, SendNotification::class.java)) }
        navLeaveApprovals.setOnClickListener { startActivity(Intent(this, LeaveApprovals::class.java)) }
        tvHeaderAvatar.setOnClickListener { startActivity(Intent(this, Profile::class.java)) }
        navProductivity.setOnClickListener { startActivity(Intent(this, Productivity::class.java)) }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Bind Views
        etWorkActivity = findViewById(R.id.etWorkActivity)
        etTargetQuantity = findViewById(R.id.etTargetQuantity)
        spUnit = findViewById(R.id.spUnit)
        btnSetTarget = findViewById(R.id.btnSetTarget)
        cardSetTarget = btnSetTarget.parent.parent as MaterialCardView

        tvTodayDate = findViewById(R.id.tvTodayDate)
        tvTodayStatus = findViewById(R.id.tvTodayStatus)
        tvTodayActivity = findViewById(R.id.tvTodayActivity)
        tvTodayTarget = findViewById(R.id.tvTodayTarget)
        tvTodayActual = findViewById(R.id.tvTodayActual)
        etActualQuantity = findViewById(R.id.etActualQuantity)
        btnCompleteProductivity = findViewById(R.id.btnCompleteProductivity)

        historyContainer = findViewById(R.id.historyContainer)
        tvEmptyHistory = findViewById(R.id.tvEmptyHistory)

        // Set today's date
        val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
        tvTodayDate.text = dateFormat.format(Date())

        btnSetTarget.setOnClickListener { submitTarget() }
        btnCompleteProductivity.setOnClickListener { completeDay() }

        // Initial Load
        loadProductivityData()
    }

    override fun onResume() {
        super.onResume()
        updateHeader()
    }

    private fun loadProductivityData() {
        val supervisorId = session.getUserId()
        if (supervisorId == 0) return

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.getSupervisorProductivity(supervisorId)
                if (response.isSuccessful) {
                    val list = response.body() ?: emptyList()

                    // Find if there is an "In Progress" target for TODAY
                    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                    currentTodayRecord = list.find { it.status == "In Progress" && it.date.startsWith(todayStr) }

                    // Update the "Today's Productivity" card
                    if (currentTodayRecord != null) {
                        tvTodayStatus.text = "In Progress"
                        tvTodayStatus.setBackgroundColor(Color.parseColor("#FFF3D6"))
                        tvTodayStatus.setTextColor(Color.parseColor("#8A5A00"))
                        tvTodayActivity.text = currentTodayRecord?.workActivity
                        tvTodayTarget.text = "Target: ${currentTodayRecord?.targetQuantity} ${currentTodayRecord?.unit}"

                        etActualQuantity.visibility = View.VISIBLE
                        btnCompleteProductivity.visibility = View.VISIBLE
                        btnCompleteProductivity.isEnabled = true
                    } else {
                        // If there is no "In Progress" record, check if they completed a target TODAY
                        val completedToday = list.find { it.status == "Completed" && it.date.startsWith(todayStr) }

                        if (completedToday != null) {
                            tvTodayStatus.text = "Completed"
                            tvTodayStatus.setBackgroundColor(Color.parseColor("#DCFCE7"))
                            tvTodayStatus.setTextColor(Color.parseColor("#166534"))
                            tvTodayActivity.text = completedToday.workActivity
                            tvTodayTarget.text = "Target: ${completedToday.targetQuantity} ${completedToday.unit}"
                            tvTodayActual.text = "Produced: ${completedToday.actualQuantity} ${completedToday.unit}" // Shows the saved number!

                            etActualQuantity.visibility = View.GONE
                            btnCompleteProductivity.visibility = View.GONE
                        } else {
                            tvTodayStatus.text = "No active targets"
                            tvTodayStatus.setBackgroundColor(Color.parseColor("#F1F5F9"))
                            tvTodayStatus.setTextColor(Color.parseColor("#64748B"))
                            tvTodayActivity.text = "Assign a target below."
                            tvTodayTarget.text = "Target: —"
                            tvTodayActual.text = "Produced: Not recorded yet"

                            etActualQuantity.visibility = View.GONE
                            btnCompleteProductivity.visibility = View.GONE
                        }
                    }

                    updateHistoryUI(list)
                }
            } catch (e: Exception) {
                Toast.makeText(this@Productivity, "Failed to load data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateHistoryUI(list: List<ProductivityDto>) {
        historyContainer.removeAllViews()
        if (list.isEmpty()) {
            tvEmptyHistory.visibility = View.VISIBLE
            historyContainer.addView(tvEmptyHistory)
        } else {
            tvEmptyHistory.visibility = View.GONE
            for (item in list) {
                val view = layoutInflater.inflate(android.R.layout.simple_list_item_2, historyContainer, false)
                val text1 = view.findViewById<TextView>(android.R.id.text1)
                val text2 = view.findViewById<TextView>(android.R.id.text2)

                text1.text = "${item.workActivity} (${item.date.substringBefore("T")})"
                text1.setTextColor(Color.parseColor("#1E293B"))
                text1.setTypeface(null, Typeface.BOLD)

                text2.text = "Target: ${item.targetQuantity} | Actual: ${item.actualQuantity ?: 0.0} ${item.unit} • ${item.status}"
                text2.setTextColor(Color.parseColor("#64748B"))
                text2.setPadding(0, 10, 0, 20)

                historyContainer.addView(view)
            }
        }
    }

    private fun submitTarget() {
        val activity = etWorkActivity.text.toString().trim()
        val targetStr = etTargetQuantity.text.toString().trim()
        val unit = spUnit.selectedItem.toString()

        if (activity.isEmpty() || targetStr.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val targetQty = targetStr.toDoubleOrNull()

        if (targetQty == null) {
            Toast.makeText(this, "Invalid numbers entered", Toast.LENGTH_SHORT).show()
            return
        }

        val request = SetTargetRequest(
            supervisor_ID = session.getUserId(),
            workActivity = activity,
            targetQuantity = targetQty,
            unit = unit
        )

        btnSetTarget.isEnabled = false

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.setProductivityTarget(request)
                if (response.isSuccessful) {
                    Toast.makeText(this@Productivity, "Target set successfully!", Toast.LENGTH_SHORT).show()
                    etWorkActivity.text.clear()
                    etTargetQuantity.text.clear()
                    loadProductivityData() // Refresh
                } else {
                    Toast.makeText(this@Productivity, "Failed: ${response.errorBody()?.string()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@Productivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                btnSetTarget.isEnabled = true
            }
        }
    }

    private fun completeDay() {
        val actualStr = etActualQuantity.text.toString().trim()
        if (actualStr.isEmpty()) {
            Toast.makeText(this, "Enter quantity produced", Toast.LENGTH_SHORT).show()
            return
        }

        val actualQty = actualStr.toDoubleOrNull()
        if (actualQty == null) {
            Toast.makeText(this, "Invalid quantity", Toast.LENGTH_SHORT).show()
            return
        }

        val recordId = currentTodayRecord?.productivity_ID ?: 0
        android.util.Log.d("ProductivityDebug", "Sending complete request for ID: $recordId")

        btnCompleteProductivity.isEnabled = false
        btnCompleteProductivity.text = "Saving..."

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.completeProductivity(recordId, CompleteProductivityRequest(actualQty))
                if (response.isSuccessful) {
                    Toast.makeText(this@Productivity, "Production recorded!", Toast.LENGTH_SHORT).show()
                    etActualQuantity.text.clear()
                    loadProductivityData() // This will refresh the UI and show the recorded amount!
                } else {
                    val err = response.errorBody()?.string() ?: "Unknown error"
                    Toast.makeText(this@Productivity, "Failed: $err", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@Productivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                btnCompleteProductivity.isEnabled = true
                btnCompleteProductivity.text = "Record End-of-Day Production"
            }
        }
    }
}
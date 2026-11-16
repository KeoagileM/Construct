package com.example.construct

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.construct.api.BroadcastRequest
import com.example.construct.api.NotificationDto
import com.example.construct.api.RetrofitClient
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class SendNotification : AppCompatActivity() {

    private lateinit var rvNotifications: RecyclerView
    private lateinit var etBroadcastMessage: TextInputEditText
    private lateinit var btnSendBroadcast: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_send_notification)

        // 1. Setup Navigation
        val navHome = findViewById<LinearLayout>(R.id.navHome)
        val navSendNotification = findViewById<LinearLayout>(R.id.navSendNotification)
        val navLeaveApprovals = findViewById<LinearLayout>(R.id.navLeaveApprovals)
        val navProductivity = findViewById<LinearLayout>(R.id.navProductivity)
        val btnHeaderNotifications = findViewById<FrameLayout>(R.id.btnHeaderNotifications)
        val tvHeaderAvatar = findViewById<TextView>(R.id.tvHeaderAvatar)

        navHome.setOnClickListener { startActivity(Intent(this, SupervisorDashboard::class.java)) }
        navLeaveApprovals.setOnClickListener { startActivity(Intent(this, LeaveApprovals::class.java)) }
        navProductivity.setOnClickListener { startActivity(Intent(this, Productivity::class.java)) }
        tvHeaderAvatar.setOnClickListener { startActivity(Intent(this, Profile::class.java)) }
        navSendNotification.setOnClickListener { startActivity(Intent(this, SendNotification::class.java)) }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 2. Setup Views
        rvNotifications = findViewById(R.id.rvNotifications)
        etBroadcastMessage = findViewById(R.id.etBroadcastMessage)
        btnSendBroadcast = findViewById(R.id.btnSendBroadcast)

        rvNotifications.layoutManager = LinearLayoutManager(this)

        btnSendBroadcast.setOnClickListener {
            sendBroadcast()
        }

        // 3. Load initial list
        loadSystemNotifications()
    }

    override fun onResume() {
        super.onResume()
        updateHeader()
    }

    private fun sendBroadcast() {
        val message = etBroadcastMessage.text.toString().trim()
        if (message.isEmpty()) {
            Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show()
            return
        }

        btnSendBroadcast.isEnabled = false
        btnSendBroadcast.text = "Sending..."

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.sendBroadcast(BroadcastRequest(message = message))
                if (response.isSuccessful) {
                    Toast.makeText(this@SendNotification, "Broadcast sent to all employees!", Toast.LENGTH_SHORT).show()
                    etBroadcastMessage.text?.clear()
                    loadSystemNotifications() // Refresh the list to show the new alert
                } else {
                    Toast.makeText(this@SendNotification, "Failed to send: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@SendNotification, "Network Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                btnSendBroadcast.isEnabled = true
                btnSendBroadcast.text = "Send Notification"
            }
        }
    }

    private fun loadSystemNotifications() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.getSystemNotifications()
                val notifications = response.body() ?: emptyList()

                // Assign adapter dynamically
                rvNotifications.adapter = SystemNotificationAdapter(notifications)

            } catch (e: Exception) {
                Toast.makeText(this@SendNotification, "Failed to load alerts", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- DYNAMIC ADAPTER FOR THE RECYCLERVIEW ---
    inner class SystemNotificationAdapter(private val notifications: List<NotificationDto>) : RecyclerView.Adapter<SystemNotificationAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            // Create the Card
            val card = MaterialCardView(parent.context)
            val params = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 30) }
            card.layoutParams = params
            card.radius = 24f
            card.cardElevation = 2f
            card.setStrokeColor(Color.parseColor("#E2E8F0"))
            card.setCardBackgroundColor(Color.WHITE)
            card.strokeWidth = 2

            // Create inner layout
            val inner = LinearLayout(parent.context)
            inner.orientation = LinearLayout.VERTICAL
            inner.setPadding(40, 40, 40, 40)
            card.addView(inner)

            return ViewHolder(card)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val notification = notifications[position]
            val card = holder.itemView as MaterialCardView
            val inner = card.getChildAt(0) as LinearLayout
            inner.removeAllViews()

            // Title
            val tvTitle = TextView(card.context)
            tvTitle.text = notification.type ?: "System Alert"
            tvTitle.textSize = 16f
            tvTitle.setTypeface(null, Typeface.BOLD)
            tvTitle.setTextColor(Color.parseColor("#0F172A"))
            inner.addView(tvTitle)

            // Date
            val tvDate = TextView(card.context)
            tvDate.text = formatDate(notification.dateSent)
            tvDate.textSize = 12f
            tvDate.setTextColor(Color.parseColor("#64748B"))
            tvDate.setPadding(0, 10, 0, 10)
            inner.addView(tvDate)

            // Message
            val tvMessage = TextView(card.context)
            tvMessage.text = notification.message
            tvMessage.textSize = 14f
            tvMessage.setTextColor(Color.parseColor("#0F172A"))
            inner.addView(tvMessage)
        }

        override fun getItemCount() = notifications.size
    }

    private fun formatDate(dateStr: String?): String {
        if (dateStr == null) return "Just now"
        return try {
            val apiFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val displayFormat = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
            val date = apiFormat.parse(dateStr.substringBefore("."))
            if (date != null) displayFormat.format(date) else dateStr
        } catch (e: Exception) {
            dateStr
        }
    }
}
package com.example.construct

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class Notifications : AppCompatActivity() {

    private lateinit var notificationsContainer: LinearLayout
    private lateinit var emptyNotifications: LinearLayout
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notifications)

        session = SessionManager(this)
        notificationsContainer = findViewById(R.id.notificationsContainer)
        emptyNotifications = findViewById(R.id.emptyNotifications)

        val navLeaveRequest = findViewById<LinearLayout>(R.id.navLeaveRequest)
        val navHome = findViewById<LinearLayout>(R.id.navHome)
        val navNotifications = findViewById<LinearLayout>(R.id.navNotifications)
        val navAttendance = findViewById<LinearLayout>(R.id.navAttendance)
        val btnHeaderNotifications = findViewById<FrameLayout>(R.id.btnHeaderNotifications)
        val tvHeaderAvatar = findViewById<TextView>(R.id.tvHeaderAvatar)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        navAttendance.setOnClickListener { startActivity(Intent(this, Attendance::class.java)) }
        navHome.setOnClickListener { startActivity(Intent(this, EmployeeDashboard::class.java)) }
        navNotifications.setOnClickListener { startActivity(Intent(this, Notifications::class.java)) }
        navLeaveRequest.setOnClickListener { startActivity(Intent(this, Leaves::class.java)) }
        tvHeaderAvatar.setOnClickListener { startActivity(Intent(this, Profile::class.java)) }
        btnHeaderNotifications.setOnClickListener { startActivity(Intent(this, Notifications::class.java)) }

        loadNotifications()
    }

    override fun onResume() {
        super.onResume()
        updateHeader()
        loadNotifications()
    }



    private fun loadNotifications() {
        val userId = session.getUserId()
        if (userId == 0) return

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.getNotifications(userId)
                val notificationsList = response.body() ?: emptyList()

                notificationsContainer.removeAllViews()

                if (notificationsList.isEmpty()) {
                    emptyNotifications.visibility = View.VISIBLE
                } else {
                    emptyNotifications.visibility = View.GONE

                    for (notification in notificationsList) {
                        val card = LayoutInflater.from(this@Notifications).inflate(
                            R.layout.item_notifications,
                            notificationsContainer,
                            false
                        )

                        val tvTitle = card.findViewById<TextView>(R.id.tvNotificationTitle)
                        val tvSender = card.findViewById<TextView>(R.id.tvNotificationSender)
                        val tvDateTime = card.findViewById<TextView>(R.id.tvNotificationDateTime)
                        val tvPreview = card.findViewById<TextView>(R.id.tvNotificationPreview)
                        val tvReadStatus = card.findViewById<TextView>(R.id.tvNotificationReadStatus)
                        val unreadIndicator = card.findViewById<View>(R.id.unreadIndicator)

                        tvTitle.text = notification.title
                        tvSender.text = "Category: ${notification.type ?: "General"}"

                        val dateString = formatDate(notification.dateSent)
                        tvDateTime.text = dateString
                        tvPreview.text = notification.message

                        // Handle nullable Boolean from Database
                        val isRead = notification.isRead ?: false

                        if (isRead) {
                            tvReadStatus.text = "Read"
                            tvReadStatus.setTextColor(Color.parseColor("#64748B"))
                            unreadIndicator.visibility = View.GONE
                        } else {
                            tvReadStatus.text = "Unread"
                            tvReadStatus.setTextColor(Color.parseColor("#2563EB"))
                            unreadIndicator.visibility = View.VISIBLE
                        }

                        card.setOnClickListener {
                            // Instantly update the UI
                            if (isRead != true) {
                                tvReadStatus.text = "Read"
                                tvReadStatus.setTextColor(Color.parseColor("#64748B"))
                                unreadIndicator.visibility = View.GONE

                                // Fire off the API call to update the database
                                markAsReadInDatabase(notification.notification_ID)
                            }

                            // Open the details screen
                            val intent = Intent(this@Notifications, NotificationDetails::class.java)
                            intent.putExtra("notificationId", notification.notification_ID) // Make sure this is the right variable
                            intent.putExtra("title", notification.title)
                            intent.putExtra("message", notification.message)
                            intent.putExtra("type", notification.type)
                            intent.putExtra("date", dateString)
                            startActivity(intent)
                        }

                        notificationsContainer.addView(card)
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@Notifications, "Failed to load: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun markAsReadInDatabase(notificationId: Int) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.markNotificationRead(notificationId)

                if (!response.isSuccessful) {
                    // If the server rejects it, show the exact error on screen
                    val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                    Toast.makeText(this@Notifications, "DB Error: $errorMsg", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@Notifications, "Network Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun formatDate(dateStr: String?): String {
        if (dateStr == null) return "Unknown date"
        return try {
            // C# sends UTC ISO string (e.g., 2026-10-17T20:11:37.123)
            val apiFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val displayFormat = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.getDefault())
            val date = apiFormat.parse(dateStr.substringBefore("."))
            if (date != null) displayFormat.format(date) else dateStr
        } catch (e: Exception) {
            dateStr
        }
    }
}
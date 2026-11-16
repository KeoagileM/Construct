package com.example.construct

import android.os.Bundle
import android.content.Intent
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.card.MaterialCardView

class Notifications : AppCompatActivity() {

    private lateinit var notificationsContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notifications)

        notificationsContainer = findViewById(R.id.notificationsContainer)
        val cardApplyLeave = findViewById<MaterialCardView>(R.id.cardApplyLeave)
        val navLeaveRequest = findViewById<LinearLayout>(R.id.navLeaveRequest)
        val navHome = findViewById<LinearLayout>(R.id.navHome)
        val navNotifications = findViewById<LinearLayout>(R.id.navNotifications)


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        navHome.setOnClickListener {
            startActivity(Intent(this, EmployeeDashboard::class.java))
        }

        navNotifications.setOnClickListener {
            startActivity(Intent(this, Notifications::class.java))
        }

        cardApplyLeave.setOnClickListener {
            startActivity(Intent(this, ApplyLeave::class.java))
        }

        navLeaveRequest.setOnClickListener {
            startActivity(Intent(this, Leaves::class.java))
        }

        loadNotifications()
    }

    private fun loadNotifications() {

        notificationsContainer.removeAllViews()

        for (notification in MockNotificationRepository.notifications) {

            val card = LayoutInflater.from(this).inflate(
                R.layout.item_notifications,
                notificationsContainer,
                false
            )

            val tvTitle = card.findViewById<TextView>(R.id.tvNotificationTitle)
            val tvSender = card.findViewById<TextView>(R.id.tvNotificationSender)
            val tvDateTime = card.findViewById<TextView>(R.id.tvNotificationDateTime)
            val tvPreview = card.findViewById<TextView>(R.id.tvNotificationPreview)
            val tvReadStatus = card.findViewById<TextView>(R.id.tvNotificationReadStatus)
            val unreadIndicator = card.findViewById<android.view.View>(R.id.unreadIndicator)

            tvTitle.text = notification.title
            tvSender.text = "Sent by ${notification.sender}"
            tvDateTime.text = notification.dateTime
            tvPreview.text = notification.message

            if (notification.isRead) {
                tvReadStatus.text = "Read"
                tvReadStatus.setTextColor(
                    android.graphics.Color.parseColor("#64748B")
                )

                unreadIndicator.visibility = android.view.View.GONE

            } else {
                tvReadStatus.text = "Unread"
                tvReadStatus.setTextColor(
                    android.graphics.Color.parseColor("#2563EB")
                )

                unreadIndicator.visibility = android.view.View.VISIBLE
            }

            card.setOnClickListener {

                notification.isRead = true

                val intent = Intent(
                    this,
                    NotificationDetails::class.java
                )

                intent.putExtra("notificationId", notification.id)

                startActivity(intent)
            }

            notificationsContainer.addView(card)
        }
    }

    override fun onResume() {
        super.onResume()

        loadNotifications()
    }
}
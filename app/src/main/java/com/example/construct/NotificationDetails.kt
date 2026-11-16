package com.example.construct

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class NotificationDetails : AppCompatActivity() {

    private lateinit var tvDetailTitle: TextView
    private lateinit var tvDetailSender: TextView
    private lateinit var tvDetailDateTime: TextView
    private lateinit var tvDetailMessage: TextView
    private lateinit var tvDetailReadStatus: TextView
    private lateinit var detailReadIndicator: android.view.View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notification_details)


        findViewById<com.google.android.material.button.MaterialButton>(
            R.id.btnBackNotifications
        ).setOnClickListener {
            finish()
        }

        tvDetailTitle = findViewById(R.id.tvDetailTitle)
        tvDetailSender = findViewById(R.id.tvDetailSender)
        tvDetailDateTime = findViewById(R.id.tvDetailDateTime)
        tvDetailMessage = findViewById(R.id.tvDetailMessage)
        tvDetailReadStatus = findViewById(R.id.tvDetailReadStatus)
        detailReadIndicator = findViewById(R.id.detailReadIndicator)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val notificationId = intent.getStringExtra("notificationId")

        val notification = MockNotificationRepository.notifications.find {
            it.id == notificationId
        }

        if (notification != null) {

            notification.isRead = true

            tvDetailTitle.text = notification.title
            tvDetailSender.text = "Sent by ${notification.sender}"
            tvDetailDateTime.text = notification.dateTime
            tvDetailMessage.text = notification.message

            tvDetailReadStatus.text = "Read"
            tvDetailReadStatus.setTextColor(
                "#64748B".toColorInt()
            )

            detailReadIndicator.visibility = android.view.View.GONE
        }
    }
}
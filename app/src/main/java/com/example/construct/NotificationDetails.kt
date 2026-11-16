package com.example.construct

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class NotificationDetails : AppCompatActivity() {

    private lateinit var tvDetailTitle: TextView
    private lateinit var tvDetailSender: TextView
    private lateinit var tvDetailDateTime: TextView
    private lateinit var tvDetailMessage: TextView
    private lateinit var tvDetailReadStatus: TextView
    private lateinit var detailReadIndicator: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notification_details)

        findViewById<com.google.android.material.button.MaterialButton>(
            R.id.btnBackNotifications
        ).setOnClickListener { finish() }

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

        // Grab data from Intent
        val title = intent.getStringExtra("title") ?: "Notification"
        val message = intent.getStringExtra("message") ?: ""
        val type = intent.getStringExtra("type") ?: "General"
        val date = intent.getStringExtra("date") ?: ""

        // Populate UI
        tvDetailTitle.text = title
        tvDetailSender.text = "Category: $type"
        tvDetailDateTime.text = date
        tvDetailMessage.text = message

        // Since we marked it as read in the previous screen, we always show "Read" here
        tvDetailReadStatus.text = "Read"
        tvDetailReadStatus.setTextColor(Color.parseColor("#64748B"))
        detailReadIndicator.visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()
        updateHeader()
    }
}
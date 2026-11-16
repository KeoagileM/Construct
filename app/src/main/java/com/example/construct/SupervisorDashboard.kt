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
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView


class SupervisorDashboard : AppCompatActivity() {


    private lateinit var dots: List<ImageView>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_supervisor_dashboard)

        val cardLeaveApprovals = findViewById<MaterialCardView>(R.id.cardLeaveApprovals)
        val navHome = findViewById<LinearLayout>(R.id.navHome)
        val navSendNotification = findViewById<LinearLayout>(R.id.navSendNotification)
        val navLeaveApprovals = findViewById<LinearLayout>(R.id.navLeaveApprovals)
        val navProductivity = findViewById<LinearLayout>(R.id.navProductivity)
        val cardProductivity = findViewById<MaterialCardView>(R.id.cardSetProductivity)
        val btnCreateNotification = findViewById<MaterialButton>(R.id.btnCreateNotification)
        val tvHeaderAvatar = findViewById<TextView>(R.id.tvHeaderAvatar)
        val btnViewQr = findViewById<MaterialButton>(R.id.btnViewQr)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize scroll indicator
        dots = listOf (
            findViewById(R.id.dot1),
            findViewById(R.id.dot2),
            findViewById(R.id.dot3), findViewById(R.id.dot4)
        )

        navHome.setOnClickListener {
            startActivity(Intent(this, SupervisorDashboard::class.java))
        }

        navSendNotification.setOnClickListener {
            startActivity(Intent(this, SendNotification::class.java))
        }

        navLeaveApprovals.setOnClickListener {
            startActivity(Intent(this, LeaveApprovals::class.java))
        }

        tvHeaderAvatar.setOnClickListener {
            startActivity(Intent(this, Profile::class.java))
        }
        cardLeaveApprovals.setOnClickListener {
            startActivity(Intent(this, LeaveApprovals::class.java))
        }

        btnCreateNotification.setOnClickListener {
            startActivity(Intent(this, SendNotification::class.java))
        }

        navProductivity.setOnClickListener {
            startActivity(Intent(this, Productivity::class.java))
        }

        cardProductivity.setOnClickListener {
            startActivity(Intent(this, Productivity::class.java))
        }
        btnViewQr.setOnClickListener {
            startActivity(Intent(this, EmployeeScanner::class.java))
        }

        updateHeader()
        setupScrollIndicator()
        setupLogout()

    }

    private fun setupLogout() {
        val btnLogout = findViewById<MaterialButton>(
            R.id.btnLogout
        )

        btnLogout.setOnClickListener {
            SessionManager(this).clear()

            Toast.makeText(
                this,
                "You have been logged out",
                Toast.LENGTH_SHORT
            ).show()

            val intent = Intent(this, Login::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            startActivity(intent)
            finish()
        }
    }
    private fun setupScrollIndicator() {
        val statsScrollView = findViewById<android.widget.HorizontalScrollView>(
            R.id.statsScrollView
        )
        statsScrollView.setOnScrollChangeListener { _, scrollX, _, _, _ ->
            // Convert 152dp card width into pixels
            val cardWidthPx = (152 * resources.displayMetrics.density).toInt()
            val position = (scrollX + (cardWidthPx / 2)) / cardWidthPx
            val activeIndex = position.coerceIn(0, 3)
            dots.forEachIndexed { index, dot ->
                if (index == activeIndex) {
                    dot.setImageResource(R.drawable.ic_dot_active) }
                else {
                    dot.setImageResource(R.drawable.ic_dot_inactive)
                }
            }
        }
    }
}
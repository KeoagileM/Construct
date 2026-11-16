package com.example.construct

import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import android.content.Intent
import android.widget.LinearLayout
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class EmployeeDashboard : AppCompatActivity() {

    private lateinit var dots: List<ImageView>
    private lateinit var btnViewQr: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_employee_dashboard)

        val cardApplyLeave = findViewById<MaterialCardView>(R.id.cardApplyLeave)
        val navLeaveRequest = findViewById<LinearLayout>(R.id.navLeaveRequest)
        val navHome = findViewById<LinearLayout>(R.id.navHome)
        val navNotifications = findViewById<LinearLayout>(R.id.navNotifications)


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

        // Initialize QR button
        btnViewQr = findViewById(R.id.btnViewQr)

        btnViewQr.setOnClickListener {
            val intent = Intent(this, EmployeeQR::class.java)
            startActivity(intent)
        }



        setupScrollIndicator()

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

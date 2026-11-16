package com.example.construct

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Intent
import android.widget.LinearLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton


class Leaves : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_leaves)

        val navLeaveRequest = findViewById<LinearLayout>(R.id.navLeaveRequest)
        val fabAddLeave = findViewById<FloatingActionButton>(R.id.fabAddLeave)
        val navHome = findViewById<LinearLayout>(R.id.navHome)
        val navNotifications = findViewById<LinearLayout>(R.id.navNotifications)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        navNotifications.setOnClickListener {
            startActivity(Intent(this, Notifications::class.java))
        }

        navHome.setOnClickListener {
            startActivity(Intent(this, EmployeeDashboard::class.java))
        }

        navLeaveRequest.setOnClickListener {
            startActivity(Intent(this, Leaves::class.java))
        }

        fabAddLeave.setOnClickListener {
            startActivity(Intent(this, ApplyLeave::class.java))
        }
    }
}
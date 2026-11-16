package com.example.construct

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Intent
import android.widget.LinearLayout
import com.google.android.material.button.MaterialButton

class ApplyLeave : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_apply_leave)

        val navLeaveRequest = findViewById<LinearLayout>(R.id.navLeaveRequest)
        val btnSubmitLeave = findViewById<MaterialButton>(R.id.btnSubmitLeave)
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
        btnSubmitLeave.setOnClickListener {
            startActivity(Intent(this, Leaves::class.java))
        }

        navLeaveRequest.setOnClickListener {
            startActivity(Intent(this, Leaves::class.java))
        }
    }
}
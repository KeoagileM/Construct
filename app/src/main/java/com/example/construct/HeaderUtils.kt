package com.example.construct

import android.app.Activity
import android.content.Intent
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.construct.api.RetrofitClient
import kotlinx.coroutines.launch

fun Activity.updateHeader() {
    val session = SessionManager(this)
    val fullName = session.getUserName().trim()

    // 1. Update Avatar Initials
    val avatar = findViewById<TextView>(R.id.tvHeaderAvatar)
    if (avatar != null) {
        val initials = fullName
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .take(2)
            .map { it.first().uppercaseChar() }
            .joinToString("")
        avatar.text = initials.ifEmpty { "?" }

        // Setup avatar click
        avatar.setOnClickListener {
            startActivity(Intent(this, Profile::class.java))
        }
    }

    // 2. Update Notification Badge
    val badge = findViewById<TextView>(R.id.tvBadgeCount)
    val userId = session.getUserId()

    if (userId != 0) {
        // This casting allows us to use lifecycleScope inside an extension function
        (this as? LifecycleOwner)?.lifecycleScope?.launch {
            try {
                val response = RetrofitClient.api.getNotifications(userId)
                val notifications = response.body() ?: emptyList()

                // Count how many are unread
                val unreadCount = notifications.count { it.isRead == false }

                if (badge != null) {
                    if (unreadCount > 0) {
                        badge.visibility = View.VISIBLE
                        badge.text = if (unreadCount > 9) "9+" else unreadCount.toString()
                    } else {
                        badge.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                if (badge != null) badge.visibility = View.GONE
            }
        }
    }

    // 3. Setup Bell Click
    val bell = findViewById<FrameLayout>(R.id.btnHeaderNotifications)
    bell?.setOnClickListener {
        startActivity(Intent(this, Notifications::class.java))
    }
}
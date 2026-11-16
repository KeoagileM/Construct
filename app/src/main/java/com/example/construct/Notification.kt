package com.example.construct

data class Notification(
    val id: Int,
    val title: String,
    val message: String,
    val type: String?,
    val dateSent: String?,
    var isRead: Boolean = false
)
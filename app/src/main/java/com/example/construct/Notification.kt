package com.example.construct

data class Notification (

    val id: String,
    val title: String,
    val sender: String,
    val dateTime: String,
    val message: String,
    var isRead: Boolean
)

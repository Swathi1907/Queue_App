package com.swathi.queue_app.model
data class NotificationModel(

    val _id: String,

    val title: String,

    val message: String,

    val type: String,

    val isRead: Boolean,

    val createdAt: String
)
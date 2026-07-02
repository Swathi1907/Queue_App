package com.swathi.queue_app.model

data class UserQueueMember(
    val tokenNumber: Int,
    val status: String,
    val isMe: Boolean
)
package com.swathi.queue_app.model

data class MemberModel(
    val tokenNumber: Int,
    val status: String,
    val isMe: Boolean,
    val name: String
)
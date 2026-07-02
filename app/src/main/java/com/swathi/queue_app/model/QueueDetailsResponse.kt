package com.swathi.queue_app.model

data class QueueDetailsResponse(
    val queueName: String,
    val currentToken: Int,
    val latestToken: Int,
    val queueStatus: String,
    val waitingUsers: Int
)
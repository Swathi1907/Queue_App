package com.swathi.queue_app.model

data class adminactivequeues(
    val queueId: String,
    val queueName: String,
    val status: String,
    val waitingCount: Int,
    val servingToken: Int?
)
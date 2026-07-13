package com.swathi.queue_app.model

data class ActiveQueueResponse(
    val active: Boolean,
    val queueId: String,
    val queueName: String?
)
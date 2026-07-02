package com.swathi.queue_app.model

data class CreateQueueRequest(
    val queueName: String,
    val queueCapacity: Int,
    val queueStatus: String
)
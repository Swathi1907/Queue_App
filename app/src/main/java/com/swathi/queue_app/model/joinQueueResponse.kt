package com.swathi.queue_app.model

data class joinQueueResponse (
    val message: String,
    val queueId: String,
    val tokenNumber: Int,
    val username: String

)
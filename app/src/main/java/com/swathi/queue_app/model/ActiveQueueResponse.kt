package com.swathi.queue_app.model

data class ActiveQueueResponse(

    val hospitalId: String,
    val hospitalName: String,

    val queueId: String,
    val queueName: String,

    val yourToken: Int,

    val peopleAhead: Int,
    val activeCount: Int,

    val currentToken: Int?,
    val totalPeople: Int,
    val latestToken: Int,

    val lastCompletedToken: Int,

    val status: String,
    val avgServiceTime: Int,

    val progress: Int,
    val queueStarted: Boolean,

    val queue_status: String,
    val QueueStatus: String
)
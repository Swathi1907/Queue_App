package com.swathi.queue_app.model
data class myStatusResponse (
    val esttime: Int,
    val queueName: String,
    val yourToken: Int,
    val latestToken: Int,
    val peopleAhead: Int,
    val activeCount: Int,
    val currentToken: Int?,
    val totalPeople: Int,
    val lastCompletedToken: Int,
    val status: String,
    val avgServiceTime: Int,
    val userId: String,
    val progress: Int,
    val queueStarted: Boolean,
val queue_status:String,
    val QueueStatus: String,
    )
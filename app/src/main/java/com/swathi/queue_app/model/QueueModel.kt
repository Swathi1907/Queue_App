package com.swathi.queue_app.model

data class QueueModel(
    val _id: String,
    val queueName: String,
    val latestToken: Int,
    val queueStatus: String,
    val activeCount: Int,
    val totalPeople: Int,
    val currentToken: Int?,
    val lastCompletedToken: Int,
    val avgServiceTime: Int,
    val queueCapacity: Int,
    val doctorName: String,
    val roomNumber: String,
    val floor: String,
    val startTime: String,
    val endTime: String,
    val waiting_members: Int,
)
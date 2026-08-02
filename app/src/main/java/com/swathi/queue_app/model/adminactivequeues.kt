package com.swathi.queue_app.model

data class adminactivequeues(
    val doctorName:String,
    val queueId: String,
    val queueName: String,

    val status: String,
    val waitingCount: Int,
    val servingToken: Int?
)

/*  queueName: queue.queueName,
            avgServiceTime: queue.avgServiceTime,
            currentToken,
            latestToken,
            queueStatus: queue.queueStatus,
            waitingUsers*/
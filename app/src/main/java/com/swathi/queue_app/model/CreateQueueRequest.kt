package com.swathi.queue_app.model

data class CreateQueueRequest(
    val queueName: String,
    val queueCapacity: Int,
    val queueStatus: String,
    val hospitalId: String,
    val doctorName: String,
   val  roomNumber:String,
  val   floor:String,
   val  startTime: String,
   val  endTime:String
)
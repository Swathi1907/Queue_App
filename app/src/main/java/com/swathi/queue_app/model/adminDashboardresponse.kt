package com.swathi.queue_app.model

data class adminDashboardresponse (
    val activeQueues: Int,
    val peopleWaiting: Int,
    val servedToday : Int,
    val avgWaitTime: Int,
)
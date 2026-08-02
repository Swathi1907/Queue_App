package com.swathi.queue_app.model

data class adminDashboardresponse(
  val hospital: HospitalInfo,
  val activeQueues: Int,
  val peopleWaiting: Int,
  val servedToday: Int,
  val pausedQueues: Int
)
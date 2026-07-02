package com.swathi.queue_app.model

data class Loginresponse(
    val message: String,
    val jwt_token : String,
    val name: String,
    val role:String
)
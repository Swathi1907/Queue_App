package com.swathi.queue_app.repository

import com.swathi.queue_app.api.RetrofitInstance
import com .swathi.queue_app.model.Loginrequest
import com.swathi.queue_app.model.signuprequest

class Authrepository {
    suspend fun login(
        email: String,
        password: String
    )= RetrofitInstance.api.login( //This calls Retrofit.
        Loginrequest(
            email,
            password
        ).also{
            println("Repository called")
        }
    )
    suspend fun signup(
        name: String,
        email: String,
        password: String
    )= RetrofitInstance.api.signup( //This calls Retrofit.
        signuprequest(
            name,
            email,
            password
        ).also{
            println("Repository called")
        }
    )
    suspend fun getProfile() =
       RetrofitInstance. api.getProfile()
}

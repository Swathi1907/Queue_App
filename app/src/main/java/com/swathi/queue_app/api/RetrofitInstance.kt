package com.swathi.queue_app.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.content.Context
import com.swathi.queue_app.MyApplication
import okhttp3.OkHttpClient

object RetrofitInstance {


    private const val BASE_URL = "http://192.168.29.181:3000"
    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->

            val prefs =
                MyApplication.instance
                    .getSharedPreferences(
                        "app",
                        Context.MODE_PRIVATE
                    )

            val token =
                prefs.getString(
                    "jwt_token",
                    null
                )

            val request =
                chain.request()
                    .newBuilder()
                    .apply {

                        token?.let {
                            addHeader(
                                "Authorization",
                                "Bearer $it"
                            )
                        }
                    }
                    .build()

            chain.proceed(request)
        }
        .build()

    val api: Apiservice by lazy {

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()
            .create(Apiservice::class.java)
    }
}
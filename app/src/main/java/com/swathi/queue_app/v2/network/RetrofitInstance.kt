package com.swathi.queue_app.v2.network

import android.content.Context
import com.swathi.queue_app.MyApplication
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
//172.22.23.34
object RetrofitInstance {
//172.22.20.42
    const val BASE_URL = "http://172.22.23.34:5000/"

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val prefs = MyApplication.instance
                .getSharedPreferences("app", Context.MODE_PRIVATE)

            val token = prefs.getString("jwt_token", null)

            val request = chain.request()
                .newBuilder()
                .apply {
                    token?.let {
                        addHeader("Authorization", "Bearer $it")
                    }
                }
                .build()

            chain.proceed(request)
        }
        .build()

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
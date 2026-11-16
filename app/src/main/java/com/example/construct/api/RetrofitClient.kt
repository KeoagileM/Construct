package com.example.construct.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // THE LIVE AZURE API URL
    private const val BASE_URL = "https://construct360-api-e8c8ddffa8hkarav.southafricanorth-01.azurewebsites.net/"

    // Holds the current JWT token. Set after successful login.
    @Volatile
    var authToken: String? = null

    val api: ConstructApi by lazy {

        // 1. Logging (helpful for debugging — remove before shipping)
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // 2. HTTP Client with auth interceptor
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val original = chain.request()
                val builder = original.newBuilder()

                authToken?.let {
                    builder.header("Authorization", "Bearer $it")
                }

                builder.header("Content-Type", "application/json")
                chain.proceed(builder.build())
            }
            .addInterceptor(logging)
            .build()

        // 3. Retrofit
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ConstructApi::class.java)
    }
}
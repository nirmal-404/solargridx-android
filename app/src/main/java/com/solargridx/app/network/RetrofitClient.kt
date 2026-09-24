package com.solargridx.app.network

import android.content.Context
import com.solargridx.app.utils.SessionManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    const val BASE_URL = "http://10.0.2.2:5205/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val baseOkHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val authApiService: AuthApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(baseOkHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApiService::class.java)
    }

    fun <T> createAuthenticatedService(context: Context, serviceClass: Class<T>): T {
        val sessionManager = SessionManager(context)
        val authInterceptor = Interceptor { chain ->
            val token = sessionManager.fetchAuthToken()
            val request = if (!token.isNullOrBlank()) {
                val bearerHeader = if (token.startsWith("Bearer ", ignoreCase = true)) token else "Bearer $token"
                chain.request().newBuilder()
                    .addHeader("Authorization", bearerHeader)
                    .build()
            } else {
                chain.request()
            }
            chain.proceed(request)
        }

        val authenticatedClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(authenticatedClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(serviceClass)
    }
}

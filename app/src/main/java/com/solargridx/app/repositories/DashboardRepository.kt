package com.solargridx.app.repositories

import android.content.Context
import com.solargridx.app.models.DashboardSummary
import com.solargridx.app.network.DashboardApiService
import com.solargridx.app.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class DashboardRepository(context: Context) {

    private val sessionManager = SessionManager(context)

    private val authInterceptor = Interceptor { chain ->
        val token = sessionManager.fetchAuthToken()
        val request = if (!token.isNullOrBlank()) {
            val bearerToken = if (token.startsWith("Bearer ")) token else "Bearer $token"
            chain.request().newBuilder()
                .addHeader("Authorization", bearerToken)
                .build()
        } else {
            chain.request()
        }
        chain.proceed(request)
    }

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val api: DashboardApiService = Retrofit.Builder()
        .baseUrl("http://10.0.2.2:5205/")
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(DashboardApiService::class.java)

    suspend fun getDashboardSummary(): Result<DashboardSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val response = api.getDashboardSummary()
            if (response.isSuccessful) {
                response.body() ?: DashboardSummary()
            } else {
                error("Server returned ${response.code()}: ${response.message()}")
            }
        }
    }
}

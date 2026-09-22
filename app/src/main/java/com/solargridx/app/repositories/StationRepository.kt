package com.solargridx.app.repositories

/**
 * StationRepository.kt
 * Purpose : Data-access layer for station operations on the Android side.
 *           Wraps StationApiService and translates HTTP responses into
 *           Kotlin Result<T> values, keeping network error handling out of
 *           the ViewModel and off the UI thread.
 *           Injects the JWT token from SessionManager into every request
 *           via OkHttp interceptor (AuthInterceptor pattern).
 * Author  : Member 2
 * Date    : 2026-09-21
 */

import android.content.Context
import com.solargridx.app.models.Station
import com.solargridx.app.network.StationApiService
import com.solargridx.app.utils.SessionManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class StationRepository(context: Context) {

    private val sessionManager = SessionManager(context)

    /** OkHttp interceptor that attaches the stored JWT to every request. */
    private val authInterceptor = Interceptor { chain ->
        // Retrieve the current session token from local SQLite storage.
        val token = sessionManager.fetchAuthToken()
        val request = if (!token.isNullOrBlank()) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        chain.proceed(request)
    }

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val api: StationApiService = Retrofit.Builder()
        .baseUrl("http://10.0.2.2:5205/")   // 10.0.2.2 maps to localhost on the Android emulator.
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(StationApiService::class.java)

    /** Fetches all active stations and wraps the result in a Kotlin Result. */
    suspend fun getStations(includeInactive: Boolean = false): Result<List<Station>> =
        runCatching {
            val response = api.getStations(includeInactive)
            if (response.isSuccessful) {
                response.body() ?: emptyList()
            } else {
                // Surface the HTTP status code as a readable error message.
                error("Server returned ${response.code()}: ${response.message()}")
            }
        }

    /** Fetches a single station by its business identifier. */
    suspend fun getStation(stationId: String): Result<Station> =
        runCatching {
            val response = api.getStation(stationId)
            if (response.isSuccessful) {
                response.body() ?: error("Empty response body for station $stationId")
            } else {
                error("Server returned ${response.code()}: ${response.message()}")
            }
        }

    /** Fetches active stations within radiusKm of the given coordinates. */
    suspend fun getNearbyStations(
        latitude: Double,
        longitude: Double,
        radiusKm: Double,
    ): Result<List<Station>> =
        runCatching {
            val response = api.getNearby(latitude, longitude, radiusKm)
            if (response.isSuccessful) {
                response.body() ?: emptyList()
            } else {
                error("Server returned ${response.code()}: ${response.message()}")
            }
        }
}

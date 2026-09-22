package com.solargridx.app.repositories

import android.content.Context
import com.google.gson.JsonParser
import com.solargridx.app.models.CreateReservationRequest
import com.solargridx.app.models.ReservationResponse
import com.solargridx.app.models.Slot
import com.solargridx.app.network.ReservationApiService
import com.solargridx.app.utils.SessionManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * ReservationRepository.kt
 * Handles network requests for slots and reservation creation, injecting the stored JWT token.
 */
class ReservationRepository(context: Context) {

    private val sessionManager = SessionManager(context)

    /** Interceptor to attach the stored JWT Bearer token to all outgoing HTTP requests */
    private val authInterceptor = Interceptor { chain ->
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

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val api: ReservationApiService = Retrofit.Builder()
        .baseUrl("http://10.0.2.2:5205/")
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ReservationApiService::class.java)

    /** Fetches energy slots for the specified station ID. */
    suspend fun getSlotsForStation(stationId: String): Result<List<Slot>> = runCatching {
        val response = api.getSlotsForStation(stationId)
        if (!response.isSuccessful || response.body() == null) {
            val fallbackResponse = api.getSlotsByQuery(stationId)
            if (fallbackResponse.isSuccessful && fallbackResponse.body() != null) {
                return@runCatching fallbackResponse.body()!!
            }
        }
        if (response.isSuccessful) {
            response.body() ?: emptyList()
        } else {
            val errorRaw = response.errorBody()?.string() ?: ""
            val message = parseErrorMessage(errorRaw) ?: "Server returned status ${response.code()}"
            error(message)
        }
    }

    /** Submits a reservation request to POST /api/reservations */
    suspend fun createReservation(request: CreateReservationRequest): Result<ReservationResponse> = runCatching {
        val response = api.createReservation(request)
        if (response.isSuccessful) {
            response.body() ?: ReservationResponse(message = "Reservation created successfully")
        } else {
            val errorRaw = response.errorBody()?.string() ?: ""
            val message = parseErrorMessage(errorRaw) ?: "Failed to create reservation (${response.code()})"
            error(message)
        }
    }

    private fun parseErrorMessage(rawJson: String): String? {
        if (rawJson.isBlank()) return null
        return try {
            val jsonObject = JsonParser.parseString(rawJson).asJsonObject
            when {
                jsonObject.has("message") -> jsonObject.get("message").asString
                jsonObject.has("Message") -> jsonObject.get("Message").asString
                jsonObject.has("error") -> jsonObject.get("error").asString
                jsonObject.has("title") -> jsonObject.get("title").asString
                jsonObject.has("detail") -> jsonObject.get("detail").asString
                else -> rawJson
            }
        } catch (e: Exception) {
            rawJson
        }
    }
}

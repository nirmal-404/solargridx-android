package com.solargridx.app.repositories

import android.content.Context
import com.google.gson.JsonParser
import com.solargridx.app.models.CreateReservationRequest
import com.solargridx.app.models.QrTokenResponse
import com.solargridx.app.models.ReservationResponse
import com.solargridx.app.models.Slot
import com.solargridx.app.models.StationResponse
import com.solargridx.app.models.UpdateReservationRequest
import com.solargridx.app.network.ReservationApiService
import com.solargridx.app.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ReservationRepository.kt
 * Handles network requests for reservations, slots, and QR tokens, injecting the stored JWT token.
 */
class ReservationRepository(context: Context) {

    private val api: ReservationApiService = RetrofitClient.createAuthenticatedService(
        context,
        ReservationApiService::class.java
    )

    /** Fetches energy slots for the specified station ID. */
    suspend fun getSlotsForStation(stationId: String): Result<List<Slot>> = withContext(Dispatchers.IO) {
        runCatching {
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
    }

    /** Submits a reservation request to POST /api/reservations */
    suspend fun createReservation(request: CreateReservationRequest): Result<ReservationResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val response = api.createReservation(request)
            if (response.isSuccessful) {
                response.body() ?: ReservationResponse(message = "Reservation created successfully")
            } else {
                val errorRaw = response.errorBody()?.string() ?: ""
                val message = parseErrorMessage(errorRaw) ?: "Failed to create reservation (${response.code()})"
                error(message)
            }
        }
    }

    /** Lists reservations with optional filters (e.g. group=pending/history/current) */
    suspend fun getReservations(
        group: String? = null,
        status: String? = null,
        stationId: String? = null,
        nic: String? = null
    ): Result<List<ReservationResponse>> = withContext(Dispatchers.IO) {
        runCatching {
            val response = api.getReservations(group = group, status = status, stationId = stationId, nic = nic)
            if (response.isSuccessful) {
                response.body() ?: emptyList()
            } else {
                val errorRaw = response.errorBody()?.string() ?: ""
                val message = parseErrorMessage(errorRaw) ?: "Failed to fetch reservations (${response.code()})"
                error(message)
            }
        }
    }

    suspend fun getPendingReservations(): Result<List<ReservationResponse>> = withContext(Dispatchers.IO) {
        runCatching {
            val response = api.getPendingReservations()
            if (response.isSuccessful) {
                response.body() ?: emptyList()
            } else {
                val errorRaw = response.errorBody()?.string() ?: ""
                val message = parseErrorMessage(errorRaw) ?: "Failed to fetch pending reservations (${response.code()})"
                error(message)
            }
        }
    }

    suspend fun getHistoryReservations(): Result<List<ReservationResponse>> = withContext(Dispatchers.IO) {
        runCatching {
            val response = api.getHistoryReservations()
            if (response.isSuccessful) {
                response.body() ?: emptyList()
            } else {
                val errorRaw = response.errorBody()?.string() ?: ""
                val message = parseErrorMessage(errorRaw) ?: "Failed to fetch history reservations (${response.code()})"
                error(message)
            }
        }
    }

    suspend fun updateReservation(
        reservationId: String,
        request: UpdateReservationRequest
    ): Result<ReservationResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val response = api.updateReservation(reservationId, request)
            if (response.isSuccessful && response.body() != null) {
                response.body()!!
            } else {
                val errorRaw = response.errorBody()?.string() ?: ""
                val message = parseErrorMessage(errorRaw) ?: "Failed to update reservation (${response.code()})"
                error(message)
            }
        }
    }

    suspend fun cancelReservation(reservationId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val response = api.cancelReservation(reservationId)
            if (response.isSuccessful) {
                Unit
            } else {
                val errorRaw = response.errorBody()?.string() ?: ""
                val message = parseErrorMessage(errorRaw) ?: "Failed to cancel reservation (${response.code()})"
                error(message)
            }
        }
    }

    suspend fun approveReservation(reservationId: String): Result<ReservationResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val response = api.approveReservation(reservationId)
            if (response.isSuccessful && response.body() != null) {
                response.body()!!
            } else {
                val errorRaw = response.errorBody()?.string() ?: ""
                val message = parseErrorMessage(errorRaw) ?: "Failed to approve reservation (${response.code()})"
                error(message)
            }
        }
    }

    suspend fun rejectReservation(reservationId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val response = api.rejectReservation(reservationId)
            if (response.isSuccessful) {
                Unit
            } else {
                val errorRaw = response.errorBody()?.string() ?: ""
                val message = parseErrorMessage(errorRaw) ?: "Failed to reject reservation (${response.code()})"
                error(message)
            }
        }
    }

    suspend fun issueTransactionToken(reservationId: String): Result<QrTokenResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val response = api.issueTransactionToken(reservationId)
            if (response.isSuccessful && response.body() != null) {
                response.body()!!
            } else {
                val errorRaw = response.errorBody()?.string() ?: ""
                val message = parseErrorMessage(errorRaw) ?: "Failed to get QR token (${response.code()})"
                error(message)
            }
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

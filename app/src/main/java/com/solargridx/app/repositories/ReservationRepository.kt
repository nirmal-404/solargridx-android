package com.solargridx.app.repositories

import com.google.gson.JsonParser
import com.solargridx.app.models.*
import com.solargridx.app.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ReservationRepository {

    private val apiService = RetrofitClient.reservationApiService

    suspend fun createReservation(request: CreateReservationRequest): Result<ReservationResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.createReservation(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorRaw = response.errorBody()?.string() ?: ""
                val msg = parseErrorMessage(errorRaw) ?: "Failed to create reservation (HTTP ${response.code()})"
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Connection error: ${e.localizedMessage ?: "Unable to reach server"}"))
        }
    }

    suspend fun getPendingReservations(): Result<List<ReservationResponse>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getPendingReservations()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorRaw = response.errorBody()?.string() ?: ""
                val msg = parseErrorMessage(errorRaw) ?: "Failed to load pending queue (HTTP ${response.code()})"
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBookingHistory(): Result<List<ReservationResponse>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getBookingHistory()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorRaw = response.errorBody()?.string() ?: ""
                val msg = parseErrorMessage(errorRaw) ?: "Failed to load history (HTTP ${response.code()})"
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getReservation(reservationId: String): Result<ReservationResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getReservation(reservationId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorRaw = response.errorBody()?.string() ?: ""
                val msg = parseErrorMessage(errorRaw) ?: "Failed to load reservation (HTTP ${response.code()})"
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchReservations(
        group: String? = null,
        status: String? = null,
        stationId: String? = null,
        nic: String? = null,
        from: String? = null,
        to: String? = null
    ): Result<List<ReservationResponse>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.searchReservations(group, status, stationId, nic, from, to)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorRaw = response.errorBody()?.string() ?: ""
                val msg = parseErrorMessage(errorRaw) ?: "Search failed (HTTP ${response.code()})"
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateReservation(
        reservationId: String,
        request: UpdateReservationRequest
    ): Result<ReservationResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.updateReservation(reservationId, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorRaw = response.errorBody()?.string() ?: ""
                val msg = parseErrorMessage(errorRaw) ?: "Failed to update reservation (HTTP ${response.code()})"
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelReservation(reservationId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.cancelReservation(reservationId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorRaw = response.errorBody()?.string() ?: ""
                val msg = parseErrorMessage(errorRaw) ?: "Failed to cancel reservation (HTTP ${response.code()})"
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun approveReservation(reservationId: String): Result<ReservationResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.approveReservation(reservationId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorRaw = response.errorBody()?.string() ?: ""
                val msg = parseErrorMessage(errorRaw) ?: "Approval failed (HTTP ${response.code()})"
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rejectReservation(reservationId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.rejectReservation(reservationId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorRaw = response.errorBody()?.string() ?: ""
                val msg = parseErrorMessage(errorRaw) ?: "Rejection failed (HTTP ${response.code()})"
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDashboardSummary(): Result<DashboardSummaryResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getDashboardSummary()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorRaw = response.errorBody()?.string() ?: ""
                val msg = parseErrorMessage(errorRaw) ?: "Failed to load dashboard summary (HTTP ${response.code()})"
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
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
                else -> rawJson
            }
        } catch (e: Exception) {
            rawJson
        }
    }
}

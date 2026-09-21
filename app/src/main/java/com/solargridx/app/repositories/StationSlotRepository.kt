package com.solargridx.app.repositories

import com.google.gson.JsonParser
import com.solargridx.app.models.SlotResponse
import com.solargridx.app.models.StationResponse
import com.solargridx.app.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StationSlotRepository {

    private val apiService = RetrofitClient.stationSlotApiService

    suspend fun getStations(includeInactive: Boolean = false): Result<List<StationResponse>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getStations(includeInactive)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorRaw = response.errorBody()?.string() ?: ""
                val msg = parseErrorMessage(errorRaw) ?: "Failed to load stations (HTTP ${response.code()})"
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSlotsByStation(stationId: String, includeInactive: Boolean = false): Result<List<SlotResponse>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getSlotsByStation(stationId, includeInactive)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorRaw = response.errorBody()?.string() ?: ""
                val msg = parseErrorMessage(errorRaw) ?: "Failed to load station slots (HTTP ${response.code()})"
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSlots(stationId: String? = null, includeInactive: Boolean = false): Result<List<SlotResponse>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getSlots(stationId, includeInactive)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorRaw = response.errorBody()?.string() ?: ""
                val msg = parseErrorMessage(errorRaw) ?: "Failed to load slots (HTTP ${response.code()})"
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

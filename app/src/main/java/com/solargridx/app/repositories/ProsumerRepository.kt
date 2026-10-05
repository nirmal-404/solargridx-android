package com.solargridx.app.repositories

import android.content.Context
import com.google.gson.JsonParser
import com.solargridx.app.models.UpdateProsumerRequest
import com.solargridx.app.models.User
import com.solargridx.app.network.ProsumerApiService
import com.solargridx.app.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProsumerRepository(context: Context) {

    private val api: ProsumerApiService = RetrofitClient.createAuthenticatedService(
        context,
        ProsumerApiService::class.java
    )

    suspend fun getProfile(nic: String): Result<User> = withContext(Dispatchers.IO) {
        runCatching {
            val response = api.getProfile(nic)
            if (response.isSuccessful && response.body() != null) {
                response.body()!!
            } else {
                val errorRaw = response.errorBody()?.string() ?: ""
                val msg = parseErrorMessage(errorRaw) ?: "Failed to fetch profile (HTTP ${response.code()})"
                error(msg)
            }
        }
    }

    suspend fun updateProfile(nic: String, request: UpdateProsumerRequest): Result<User> = withContext(Dispatchers.IO) {
        runCatching {
            val response = api.updateProfile(nic, request)
            if (response.isSuccessful && response.body() != null) {
                response.body()!!
            } else {
                val errorRaw = response.errorBody()?.string() ?: ""
                val msg = parseErrorMessage(errorRaw) ?: "Failed to update profile (HTTP ${response.code()})"
                error(msg)
            }
        }
    }

    suspend fun requestDeactivation(nic: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val response = api.requestDeactivation(nic)
            if (response.isSuccessful) {
                Unit
            } else {
                val errorRaw = response.errorBody()?.string() ?: ""
                val msg = parseErrorMessage(errorRaw) ?: "Failed to request deactivation (HTTP ${response.code()})"
                error(msg)
            }
        }
    }

    suspend fun cancelDeactivation(nic: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val response = api.cancelDeactivation(nic)
            if (response.isSuccessful) {
                Unit
            } else {
                val errorRaw = response.errorBody()?.string() ?: ""
                val msg = parseErrorMessage(errorRaw) ?: "Failed to cancel deactivation (HTTP ${response.code()})"
                error(msg)
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

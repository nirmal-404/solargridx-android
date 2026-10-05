package com.solargridx.app.repositories

import com.google.gson.JsonParser
import com.solargridx.app.models.LoginRequest
import com.solargridx.app.models.LoginResponse
import com.solargridx.app.models.User
import com.solargridx.app.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository {

    private val apiService = RetrofitClient.authApiService

    suspend fun login(request: LoginRequest): Result<LoginResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.login(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorRaw = response.errorBody()?.string() ?: ""
                val parsedMessage = parseErrorMessage(errorRaw) ?: "Login failed (HTTP ${response.code()})"
                Result.failure(Exception(parsedMessage))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Connection error: ${e.localizedMessage ?: "Unable to reach server at http://10.0.2.2:5205"}"))
        }
    }

    suspend fun register(request: com.solargridx.app.models.RegisterRequest): Result<User> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.register(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorRaw = response.errorBody()?.string() ?: ""
                val parsedMessage = parseErrorMessage(errorRaw) ?: "Registration failed (HTTP ${response.code()})"
                Result.failure(Exception(parsedMessage))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Connection error: ${e.localizedMessage ?: "Unable to reach server at http://10.0.2.2:5205"}"))
        }
    }

    suspend fun getCurrentUser(token: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            val authToken = if (token.startsWith("Bearer ")) token else "Bearer $token"
            val response = apiService.getCurrentUser(authToken)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorRaw = response.errorBody()?.string() ?: ""
                val parsedMessage = parseErrorMessage(errorRaw) ?: "Failed to retrieve profile (HTTP ${response.code()})"
                Result.failure(Exception(parsedMessage))
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

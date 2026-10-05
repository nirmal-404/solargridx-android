package com.solargridx.app.repositories

import android.content.Context
import com.google.gson.JsonParser
import com.solargridx.app.models.TransactionResponse
import com.solargridx.app.models.VerifyTransactionRequest
import com.solargridx.app.network.RetrofitClient
import com.solargridx.app.network.TransactionApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TransactionRepository(context: Context) {

    private val api: TransactionApiService = RetrofitClient.createAuthenticatedService(
        context,
        TransactionApiService::class.java
    )

    suspend fun verifyTransaction(token: String): Result<TransactionResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val response = api.verifyTransaction(VerifyTransactionRequest(token = token))
            if (response.isSuccessful && response.body() != null) {
                response.body()!!
            } else {
                val errorRaw = response.errorBody()?.string() ?: ""
                val msg = parseErrorMessage(errorRaw) ?: "Verification failed (HTTP ${response.code()})"
                error(msg)
            }
        }
    }

    suspend fun completeTransaction(transactionId: String): Result<TransactionResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val response = api.completeTransaction(transactionId)
            if (response.isSuccessful && response.body() != null) {
                response.body()!!
            } else {
                val errorRaw = response.errorBody()?.string() ?: ""
                val msg = parseErrorMessage(errorRaw) ?: "Completion failed (HTTP ${response.code()})"
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

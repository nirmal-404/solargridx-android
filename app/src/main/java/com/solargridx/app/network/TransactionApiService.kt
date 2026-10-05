package com.solargridx.app.network

import com.solargridx.app.models.TransactionResponse
import com.solargridx.app.models.VerifyTransactionRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface TransactionApiService {

    @POST("api/transactions/verify")
    suspend fun verifyTransaction(
        @Body request: VerifyTransactionRequest
    ): Response<TransactionResponse>

    @POST("api/transactions/{transactionId}/complete")
    suspend fun completeTransaction(
        @Path("transactionId") transactionId: String
    ): Response<TransactionResponse>
}

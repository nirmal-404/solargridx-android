package com.solargridx.app.network

import com.solargridx.app.models.LoginRequest
import com.solargridx.app.models.LoginResponse
import com.solargridx.app.models.User
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApiService {

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/auth/register")
    suspend fun register(@Body request: com.solargridx.app.models.RegisterRequest): Response<User>

    @GET("api/auth/me")
    suspend fun getCurrentUser(
        @Header("Authorization") token: String
    ): Response<User>
}

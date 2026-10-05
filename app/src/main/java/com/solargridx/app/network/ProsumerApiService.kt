package com.solargridx.app.network

import com.solargridx.app.models.UpdateProsumerRequest
import com.solargridx.app.models.User
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ProsumerApiService {

    @GET("api/prosumers/{nic}")
    suspend fun getProfile(
        @Path("nic") nic: String
    ): Response<User>

    @PUT("api/prosumers/{nic}")
    suspend fun updateProfile(
        @Path("nic") nic: String,
        @Body request: UpdateProsumerRequest
    ): Response<User>

    @POST("api/prosumers/{nic}/deactivation-request")
    suspend fun requestDeactivation(
        @Path("nic") nic: String
    ): Response<Void>

    @POST("api/prosumers/{nic}/cancel-deactivation")
    suspend fun cancelDeactivation(
        @Path("nic") nic: String
    ): Response<Void>
}

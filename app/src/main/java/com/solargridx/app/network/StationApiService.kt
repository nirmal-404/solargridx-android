package com.solargridx.app.network

/**
 * StationApiService.kt
 * Purpose : Retrofit interface defining the station-related API endpoints.
 *           Follows the same pattern as AuthApiService — one interface per domain.
 *           The JWT token is injected by the AuthInterceptor added to OkHttpClient
 *           in StationRetrofitClient so every call is automatically authenticated.
 * Author  : Member 2
 * Date    : 2026-09-21
 */

import com.solargridx.app.models.StationResponse
import com.solargridx.app.models.CreateStationRequest
import com.solargridx.app.models.UpdateStationRequest
import com.solargridx.app.models.UpdateStationScheduleRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

interface StationApiService {

    /** Returns all stations; includeInactive=true is Backoffice-only on the API side. */
    @GET("api/stations")
    suspend fun getStations(
        @Query("includeInactive") includeInactive: Boolean = false,
    ): Response<List<StationResponse>>

    /** Retrieves a single station by its business identifier (e.g. "SGX-01"). */
    @GET("api/stations/{stationId}")
    suspend fun getStation(
        @Path("stationId") stationId: String,
    ): Response<StationResponse>

    /** Returns active stations near the supplied coordinate pair within radiusKm. */
    @GET("api/stations/nearby")
    suspend fun getNearby(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("radiusKm") radiusKm: Double,
    ): Response<List<StationResponse>>

    // Creates a node; the API enforces Backoffice authorization and validation.
    @POST("api/stations")
    suspend fun createStation(@Body request: CreateStationRequest): Response<StationResponse>

    // Updates mutable node fields without changing its station identifier.
    @PUT("api/stations/{stationId}")
    suspend fun updateStation(
        @Path("stationId") stationId: String,
        @Body request: UpdateStationRequest,
    ): Response<StationResponse>

    // Replaces the node's weekly operating schedule.
    @PATCH("api/stations/{stationId}/schedule")
    suspend fun updateSchedule(
        @Path("stationId") stationId: String,
        @Body request: UpdateStationScheduleRequest,
    ): Response<StationResponse>

    // Deactivates a node; API returns 409 while active reservations exist.
    @POST("api/stations/{stationId}/deactivate")
    suspend fun deactivateStation(@Path("stationId") stationId: String): Response<Unit>

    // Reactivates an inactive node.
    @POST("api/stations/{stationId}/reactivate")
    suspend fun reactivateStation(@Path("stationId") stationId: String): Response<Unit>
}

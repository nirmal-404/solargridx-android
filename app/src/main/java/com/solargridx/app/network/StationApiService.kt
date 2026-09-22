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

import com.solargridx.app.models.Station
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface StationApiService {

    /** Returns all stations; includeInactive=true is Backoffice-only on the API side. */
    @GET("api/stations")
    suspend fun getStations(
        @Query("includeInactive") includeInactive: Boolean = false,
    ): Response<List<Station>>

    /** Retrieves a single station by its business identifier (e.g. "SGX-01"). */
    @GET("api/stations/{stationId}")
    suspend fun getStation(
        @Path("stationId") stationId: String,
    ): Response<Station>

    /** Returns active stations near the supplied coordinate pair within radiusKm. */
    @GET("api/stations/nearby")
    suspend fun getNearby(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("radiusKm") radiusKm: Double,
    ): Response<List<Station>>
}

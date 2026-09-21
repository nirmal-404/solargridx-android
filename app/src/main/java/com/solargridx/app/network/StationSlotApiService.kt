package com.solargridx.app.network

import com.solargridx.app.models.SlotResponse
import com.solargridx.app.models.StationResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface StationSlotApiService {

    @GET("api/stations")
    suspend fun getStations(
        @Query("includeInactive") includeInactive: Boolean = false
    ): Response<List<StationResponse>>

    @GET("api/stations/{stationId}/slots")
    suspend fun getSlotsByStation(
        @Path("stationId") stationId: String,
        @Query("includeInactive") includeInactive: Boolean = false
    ): Response<List<SlotResponse>>

    @GET("api/slots")
    suspend fun getSlots(
        @Query("stationId") stationId: String? = null,
        @Query("includeInactive") includeInactive: Boolean = false
    ): Response<List<SlotResponse>>
}


package com.solargridx.app.network

import com.solargridx.app.models.CreateReservationRequest
import com.solargridx.app.models.ReservationResponse
import com.solargridx.app.models.Slot
import com.solargridx.app.models.StationResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface matching the provided C# StationsController
 * and reservation creation endpoint.
 */
interface ReservationApiService {

    /**
     * GET /api/stations
     * Retrieves active stations for authenticated clients.
     */
    @GET("api/stations")
    suspend fun getStations(): Response<List<StationResponse>>

    /**
     * GET /api/stations/{stationId}
     * Retrieves one station.
     */
    @GET("api/stations/{stationId}")
    suspend fun getStation(
        @Path("stationId") stationId: String
    ): Response<StationResponse>

    /**
     * GET /api/stations/nearby
     * Retrieves nearby active stations.
     */
    @GET("api/stations/nearby")
    suspend fun getNearbyStations(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("radiusKm") radiusKm: Double
    ): Response<List<StationResponse>>

    /**
     * GET /api/stations/{stationId}/slots
     * Fetches energy slots for a given station ID.
     */
    @GET("api/stations/{stationId}/slots")
    suspend fun getSlotsForStation(
        @Path("stationId") stationId: String
    ): Response<List<Slot>>

    /**
     * GET /api/slots
     * Fallback endpoint for fetching energy slots by query parameter.
     */
    @GET("api/slots")
    suspend fun getSlotsByQuery(
        @Query("stationId") stationId: String
    ): Response<List<Slot>>

    /**
     * POST /api/reservations
     * Creates an energy slot reservation.
     */
    @POST("api/reservations")
    suspend fun createReservation(
        @Body request: CreateReservationRequest
    ): Response<ReservationResponse>
}
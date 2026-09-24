package com.solargridx.app.network

import com.solargridx.app.models.CreateReservationRequest
import com.solargridx.app.models.QrTokenResponse
import com.solargridx.app.models.ReservationResponse
import com.solargridx.app.models.Slot
import com.solargridx.app.models.StationResponse
import com.solargridx.app.models.UpdateReservationRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ReservationApiService {

    @GET("api/stations")
    suspend fun getStations(): Response<List<StationResponse>>

    @GET("api/stations/{stationId}")
    suspend fun getStation(
        @Path("stationId") stationId: String
    ): Response<StationResponse>

    @GET("api/stations/nearby")
    suspend fun getNearbyStations(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("radiusKm") radiusKm: Double
    ): Response<List<StationResponse>>

    @GET("api/stations/{stationId}/slots")
    suspend fun getSlotsForStation(
        @Path("stationId") stationId: String
    ): Response<List<Slot>>

    @GET("api/slots")
    suspend fun getSlotsByQuery(
        @Query("stationId") stationId: String
    ): Response<List<Slot>>

    @POST("api/reservations")
    suspend fun createReservation(
        @Body request: CreateReservationRequest
    ): Response<ReservationResponse>

    @GET("api/reservations")
    suspend fun getReservations(
        @Query("group") group: String? = null,
        @Query("status") status: String? = null,
        @Query("stationId") stationId: String? = null,
        @Query("nic") nic: String? = null,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): Response<List<ReservationResponse>>

    @GET("api/reservations/pending")
    suspend fun getPendingReservations(): Response<List<ReservationResponse>>

    @GET("api/reservations/history")
    suspend fun getHistoryReservations(): Response<List<ReservationResponse>>

    @GET("api/reservations/{reservationId}")
    suspend fun getReservation(
        @Path("reservationId") reservationId: String
    ): Response<ReservationResponse>

    @PUT("api/reservations/{reservationId}")
    suspend fun updateReservation(
        @Path("reservationId") reservationId: String,
        @Body request: UpdateReservationRequest
    ): Response<ReservationResponse>

    @POST("api/reservations/{reservationId}/cancel")
    suspend fun cancelReservation(
        @Path("reservationId") reservationId: String
    ): Response<Void>

    @POST("api/reservations/{reservationId}/approve")
    suspend fun approveReservation(
        @Path("reservationId") reservationId: String
    ): Response<ReservationResponse>

    @POST("api/reservations/{reservationId}/reject")
    suspend fun rejectReservation(
        @Path("reservationId") reservationId: String
    ): Response<Void>

    @POST("api/reservations/{reservationId}/transaction-token")
    suspend fun issueTransactionToken(
        @Path("reservationId") reservationId: String
    ): Response<QrTokenResponse>
}
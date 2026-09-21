package com.solargridx.app.network

import com.solargridx.app.models.CreateReservationRequest
import com.solargridx.app.models.DashboardSummaryResponse
import com.solargridx.app.models.ReservationResponse
import com.solargridx.app.models.UpdateReservationRequest
import retrofit2.Response
import retrofit2.http.*

interface ReservationApiService {

    @POST("api/reservations")
    suspend fun createReservation(
        @Body request: CreateReservationRequest
    ): Response<ReservationResponse>

    @GET("api/reservations/pending")
    suspend fun getPendingReservations(): Response<List<ReservationResponse>>

    @GET("api/reservations/history")
    suspend fun getBookingHistory(): Response<List<ReservationResponse>>

    @GET("api/reservations/{reservationId}")
    suspend fun getReservation(
        @Path("reservationId") reservationId: String
    ): Response<ReservationResponse>

    @GET("api/reservations")
    suspend fun searchReservations(
        @Query("group") group: String? = null,
        @Query("status") status: String? = null,
        @Query("stationId") stationId: String? = null,
        @Query("nic") nic: String? = null,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): Response<List<ReservationResponse>>

    @PUT("api/reservations/{reservationId}")
    suspend fun updateReservation(
        @Path("reservationId") reservationId: String,
        @Body request: UpdateReservationRequest
    ): Response<ReservationResponse>

    @POST("api/reservations/{reservationId}/cancel")
    suspend fun cancelReservation(
        @Path("reservationId") reservationId: String
    ): Response<Unit>

    @POST("api/reservations/{reservationId}/approve")
    suspend fun approveReservation(
        @Path("reservationId") reservationId: String
    ): Response<ReservationResponse>

    @POST("api/reservations/{reservationId}/reject")
    suspend fun rejectReservation(
        @Path("reservationId") reservationId: String
    ): Response<Unit>

    @GET("api/dashboard/summary")
    suspend fun getDashboardSummary(): Response<DashboardSummaryResponse>
}

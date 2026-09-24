package com.solargridx.app.models

import com.google.gson.annotations.SerializedName

/**
 * DashboardSummary.kt
 * Data model for GET /api/dashboard/summary API response.
 */
data class DashboardSummary(
    @SerializedName("pendingBookings", alternate = ["PendingBookings", "pendingReservations", "PendingReservations", "pendingQueue", "PendingQueue", "pendingCount", "pending"])
    val pendingBookings: Int = 0,

    @SerializedName("approvedFuture", alternate = ["ApprovedFuture", "approvedFutureReservations", "ApprovedFutureReservations", "approvedBookings", "ApprovedBookings", "approvedCount", "approved"])
    val approvedFuture: Int = 0,

    @SerializedName("activeSessions", alternate = ["ActiveSessions", "currentReservations", "CurrentReservations", "activeBookings", "ActiveBookings", "activeCount", "active"])
    val activeSessions: Int = 0,

    @SerializedName("completedBookings", alternate = ["CompletedBookings", "completedReservations", "CompletedReservations", "completedCount", "completed"])
    val completedBookings: Int = 0,

    @SerializedName("totalBookings", alternate = ["TotalBookings", "totalCount", "total"])
    val totalBookings: Int = 0,

    @SerializedName("userName", alternate = ["UserName", "user", "fullName", "FullName"])
    val userName: String? = null,

    @SerializedName("role", alternate = ["Role", "userRole"])
    val role: String? = null
)

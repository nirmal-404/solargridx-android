package com.solargridx.app.models

import com.google.gson.annotations.SerializedName

data class DashboardSummaryResponse(
    @SerializedName("pendingReservations", alternate = ["PendingReservations"])
    val pendingReservations: Int = 0,

    @SerializedName("approvedFutureReservations", alternate = ["ApprovedFutureReservations"])
    val approvedFutureReservations: Int = 0,

    @SerializedName("currentReservations", alternate = ["CurrentReservations"])
    val currentReservations: Int = 0,

    @SerializedName("completedReservations", alternate = ["CompletedReservations"])
    val completedReservations: Int = 0
)

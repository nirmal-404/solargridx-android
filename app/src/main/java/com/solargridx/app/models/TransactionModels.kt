package com.solargridx.app.models

import com.google.gson.annotations.SerializedName

data class QrTokenResponse(
    @SerializedName("transactionId") val transactionId: String?,
    @SerializedName("token") val token: String,
    @SerializedName("expiresAt") val expiresAt: String
)

data class VerifyTransactionRequest(
    @SerializedName("token") val token: String
)

data class TransactionResponse(
    @SerializedName("transactionId") val transactionId: String,
    @SerializedName("reservationId") val reservationId: String,
    @SerializedName("stationId") val stationId: String,
    @SerializedName("slotId") val slotId: String? = null,
    @SerializedName("scheduledStartTime") val scheduledStartTime: String? = null,
    @SerializedName("scheduledEndTime") val scheduledEndTime: String? = null,
    @SerializedName("requestedCapacity") val requestedCapacity: Double = 0.0,
    @SerializedName("status") val status: String = "InProgress",
    @SerializedName("expiresAt") val expiresAt: String? = null,
    @SerializedName("completedAt") val completedAt: String? = null,
    @SerializedName("prosumerNic") val prosumerNic: String? = null,
    @SerializedName("prosumerName") val prosumerName: String? = null,
    @SerializedName("stationName") val stationName: String? = null,
    @SerializedName("transferType") val transferType: String? = null
) {
    val capacityKwh: Double get() = requestedCapacity
}

data class UpdateReservationRequest(
    @SerializedName("stationId") val stationId: String,
    @SerializedName("slotId") val slotId: String,
    @SerializedName("requestedCapacity") val requestedCapacity: Double
)

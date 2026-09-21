package com.solargridx.app.models

import com.google.gson.annotations.SerializedName

data class ReservationResponse(
    @SerializedName("id", alternate = ["Id"])
    val id: String,

    @SerializedName("reservationId", alternate = ["ReservationId"])
    val reservationId: String,

    @SerializedName("stationId", alternate = ["StationId"])
    val stationId: String,

    @SerializedName("slotId", alternate = ["SlotId"])
    val slotId: String,

    @SerializedName("scheduledStartTime", alternate = ["ScheduledStartTime"])
    val scheduledStartTime: String,

    @SerializedName("scheduledEndTime", alternate = ["ScheduledEndTime"])
    val scheduledEndTime: String,

    @SerializedName("requestedCapacity", alternate = ["RequestedCapacity"])
    val requestedCapacity: Double,

    @SerializedName("status", alternate = ["Status"])
    val status: ReservationStatus,

    @SerializedName("transactionId", alternate = ["TransactionId"])
    val transactionId: String? = null,

    @SerializedName("transactionExpiresAt", alternate = ["TransactionExpiresAt"])
    val transactionExpiresAt: String? = null,

    @SerializedName("approvedAt", alternate = ["ApprovedAt"])
    val approvedAt: String? = null,

    @SerializedName("cancelledAt", alternate = ["CancelledAt"])
    val cancelledAt: String? = null,

    @SerializedName("completedAt", alternate = ["CompletedAt"])
    val completedAt: String? = null,

    @SerializedName("prosumerNic", alternate = ["ProsumerNic"])
    val prosumerNic: String? = null
)

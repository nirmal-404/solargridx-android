package com.solargridx.app.models

import com.google.gson.annotations.SerializedName

/**
 * Reservation.kt
 * Purpose : DTO request and response models for /api/reservations.
 */

/** Request body payload for creating a new slot reservation. */
data class CreateReservationRequest(
    @SerializedName("StationId", alternate = ["stationId"])
    val stationId: String,

    @SerializedName("SlotId", alternate = ["slotId"])
    val slotId: String,

    @SerializedName("RequestedCapacity", alternate = ["requestedCapacity"])
    val requestedCapacity: Double,

    @SerializedName("TransferType", alternate = ["transferType"])
    val transferType: String = "DropOff",

    @SerializedName("Notes", alternate = ["notes"])
    val notes: String? = null
)

/** Response model returned from reservation endpoints. */
data class ReservationResponse(
    @SerializedName("id", alternate = ["Id"])
    val id: String? = null,

    @SerializedName("reservationId", alternate = ["ReservationId"])
    val reservationId: String? = null,

    @SerializedName("stationId", alternate = ["StationId"])
    val stationId: String? = null,

    @SerializedName("slotId", alternate = ["SlotId"])
    val slotId: String? = null,

    @SerializedName("scheduledStartTime", alternate = ["ScheduledStartTime"])
    val scheduledStartTime: String? = null,

    @SerializedName("scheduledEndTime", alternate = ["ScheduledEndTime"])
    val scheduledEndTime: String? = null,

    @SerializedName("requestedCapacity", alternate = ["RequestedCapacity", "capacityKwh", "CapacityKwh"])
    val requestedCapacity: Double? = null,

    @SerializedName("status", alternate = ["Status"])
    val status: String? = null,

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
    val prosumerNic: String? = null,

    @SerializedName("transferType", alternate = ["TransferType"])
    val transferType: String? = null,

    @SerializedName("notes", alternate = ["Notes"])
    val notes: String? = null,

    @SerializedName("approvedByUserId", alternate = ["ApprovedByUserId"])
    val approvedByUserId: String? = null,

    @SerializedName("completedByUserId", alternate = ["CompletedByUserId"])
    val completedByUserId: String? = null,

    @SerializedName("createdAt", alternate = ["CreatedAt", "timestamp", "Timestamp"])
    val createdAt: String? = null,

    @SerializedName("message", alternate = ["Message"])
    val message: String? = null
) {
    val displayId: String
        get() = reservationId?.takeIf { it.isNotBlank() } ?: id ?: "RES-N/A"
}


package com.solargridx.app.models

import com.google.gson.annotations.SerializedName

/**
 * Reservation.kt
 * Purpose : DTO request and response models for POST /api/reservations.
 */

/** Request body payload for creating a new slot reservation. */
data class CreateReservationRequest(
    @SerializedName("StationId", alternate = ["stationId"])
    val stationId: String,

    @SerializedName("SlotId", alternate = ["slotId"])
    val slotId: String,

    @SerializedName("RequestedCapacity", alternate = ["requestedCapacity"])
    val requestedCapacity: Double
)

/** Response model returned when a reservation is successfully created (201 Created). */
data class ReservationResponse(
    @SerializedName("id", alternate = ["reservationId", "ReservationId", "Id"])
    val id: String? = null,

    @SerializedName("stationId", alternate = ["StationId"])
    val stationId: String? = null,

    @SerializedName("slotId", alternate = ["SlotId"])
    val slotId: String? = null,

    @SerializedName("requestedCapacity", alternate = ["RequestedCapacity", "capacityKwh", "CapacityKwh"])
    val requestedCapacity: Double? = null,

    @SerializedName("status", alternate = ["Status"])
    val status: String? = null,

    @SerializedName("createdAt", alternate = ["CreatedAt", "timestamp", "Timestamp"])
    val createdAt: String? = null,

    @SerializedName("message", alternate = ["Message"])
    val message: String? = null
)

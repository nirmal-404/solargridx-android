package com.solargridx.app.models

import com.google.gson.annotations.SerializedName

data class UpdateReservationRequest(
    @SerializedName("stationId")
    val stationId: String,

    @SerializedName("slotId")
    val slotId: String,

    @SerializedName("requestedCapacity")
    val requestedCapacity: Double
)

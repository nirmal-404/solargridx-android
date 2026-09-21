package com.solargridx.app.models

import com.google.gson.annotations.SerializedName

data class CreateReservationRequest(
    @SerializedName("stationId")
    val stationId: String,

    @SerializedName("slotId")
    val slotId: String,

    @SerializedName("requestedCapacity")
    val requestedCapacity: Double
)

package com.solargridx.app.models

import com.google.gson.annotations.SerializedName

data class SlotResponse(
    @SerializedName("id", alternate = ["Id"])
    val id: String,

    @SerializedName("slotId", alternate = ["SlotId"])
    val slotId: String,

    @SerializedName("stationId", alternate = ["StationId"])
    val stationId: String,

    @SerializedName("startTime", alternate = ["StartTime"])
    val startTime: String,

    @SerializedName("endTime", alternate = ["EndTime"])
    val endTime: String,

    @SerializedName("capacity", alternate = ["Capacity"])
    val capacity: Double,

    @SerializedName("availableCapacity", alternate = ["AvailableCapacity"])
    val availableCapacity: Double,

    @SerializedName("status", alternate = ["Status"])
    val status: String = "Active"
)

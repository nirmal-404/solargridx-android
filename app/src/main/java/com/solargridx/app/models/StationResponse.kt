package com.solargridx.app.models

import com.google.gson.annotations.SerializedName

data class StationResponse(
    @SerializedName("id", alternate = ["Id"])
    val id: String,

    @SerializedName("stationId", alternate = ["StationId"])
    val stationId: String,

    @SerializedName("name", alternate = ["Name"])
    val name: String,

    @SerializedName("description", alternate = ["Description"])
    val description: String? = null,

    @SerializedName("latitude", alternate = ["Latitude"])
    val latitude: Double = 0.0,

    @SerializedName("longitude", alternate = ["Longitude"])
    val longitude: Double = 0.0,

    @SerializedName("capacityKwh", alternate = ["CapacityKwh"])
    val capacityKwh: Double = 0.0,

    @SerializedName("availableBatteryStorageSlots", alternate = ["AvailableBatteryStorageSlots"])
    val availableBatteryStorageSlots: Int = 0,

    @SerializedName("status", alternate = ["Status"])
    val status: String = "Active"
)

package com.solargridx.app.models

import com.google.gson.annotations.SerializedName

data class CreateStationRequest(
    @SerializedName("stationId") val stationId: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("capacityKwh") val capacityKwh: Double,
    @SerializedName("availableBatteryStorageSlots") val availableBatteryStorageSlots: Int,
    @SerializedName("schedule") val schedule: OperationalSchedule,
)

data class UpdateStationRequest(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("capacityKwh") val capacityKwh: Double,
    @SerializedName("availableBatteryStorageSlots") val availableBatteryStorageSlots: Int,
)

data class UpdateStationScheduleRequest(
    @SerializedName("schedule") val schedule: OperationalSchedule,
)
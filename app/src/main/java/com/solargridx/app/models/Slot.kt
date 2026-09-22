package com.solargridx.app.models

import com.google.gson.annotations.SerializedName

/**
 * Slot.kt
 * Purpose : Data class representing an available energy slot for a station.
 *           Uses Gson @SerializedName annotations with fallback property names
 *           to safely handle various C# API JSON naming conventions.
 */
data class Slot(
    @SerializedName("id", alternate = ["slotId", "SlotId", "Id"])
    val id: String = "",

    @SerializedName("stationId", alternate = ["StationId"])
    val stationId: String = "",

    @SerializedName("startTime", alternate = ["Start", "startTimeUtc", "StartTime", "StartUtc"])
    val startTime: String = "",

    @SerializedName("endTime", alternate = ["End", "endTimeUtc", "EndTime", "EndUtc"])
    val endTime: String = "",

    @SerializedName("capacityKwh", alternate = ["availableCapacityKwh", "remainingCapacityKwh", "CapacityKwh", "Capacity"])
    val capacityKwh: Double? = null,

    @SerializedName("status", alternate = ["Status"])
    val status: String? = null,

    @SerializedName("isAvailable", alternate = ["available", "Available"])
    val isAvailable: Boolean? = null
) {
    /** Helper property to check if the slot is currently eligible for booking */
    val isEligible: Boolean
        get() {
            if (isAvailable == false) return false
            if (!status.isNullOrBlank()) {
                val s = status.uppercase()
                if (s == "DEACTIVATED" || s == "UNAVAILABLE" || s == "BOOKED" || s == "CLOSED" || s == "CANCELLED") {
                    return false
                }
            }
            return true
        }
}

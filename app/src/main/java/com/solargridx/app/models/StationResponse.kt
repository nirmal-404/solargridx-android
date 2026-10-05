package com.solargridx.app.models

import com.google.gson.annotations.SerializedName

/** Type alias so Station can be used interchangeably with StationResponse */
typealias Station = StationResponse

/**
 * StationResponse.kt
 * Purpose : Data class matching the SolarStation JSON response from the REST API.
 *           Includes Gson @SerializedName annotations with distinct non-overlapping
 *           PascalCase and camelCase alternate property names.
 * Author  : Member 2
 * Date    : 2026-09-21
 */

/** Represents a single day's operating window inside an OperationalSchedule. */
data class DailyHours(
    @SerializedName("day", alternate = ["Day"])
    val day: String = "",

    @SerializedName("open", alternate = ["Open"])
    val open: String = "",

    @SerializedName("close", alternate = ["Close"])
    val close: String = ""
) {
    /** Returns 0 for Sunday, 1 for Monday ... 6 for Saturday, matching DayOfWeek standard */
    val dayOfWeek: Int
        get() = when (day.trim().lowercase()) {
            "sunday", "0" -> 0
            "monday", "1" -> 1
            "tuesday", "2" -> 2
            "wednesday", "3" -> 3
            "thursday", "4" -> 4
            "friday", "5" -> 5
            "saturday", "6" -> 6
            else -> day.toIntOrNull() ?: 0
        }

    val dayName: String
        get() = when (day.trim().lowercase()) {
            "0", "sunday" -> "Sunday"
            "1", "monday" -> "Monday"
            "2", "tuesday" -> "Tuesday"
            "3", "wednesday" -> "Wednesday"
            "4", "thursday" -> "Thursday"
            "5", "friday" -> "Friday"
            "6", "saturday" -> "Saturday"
            else -> day
        }
}

/** Holds the weekly operating schedule for a station node. */
data class OperationalSchedule(
    @SerializedName("timeZoneId", alternate = ["TimeZoneId"])
    val timeZoneId: String? = null,

    @SerializedName("days", alternate = ["Days"])
    val days: List<DailyHours> = emptyList()
)

/** Top-level station response object from GET /api/stations and GET /api/stations/{id}. */
data class StationResponse(
    @SerializedName("id", alternate = ["Id", "_id", "ID"])
    val id: String = "",

    @SerializedName("stationId", alternate = ["StationId", "STATION_ID"])
    val stationId: String = "",

    @SerializedName("name", alternate = ["Name", "stationName", "StationName", "title", "Title"])
    val name: String = "",

    @SerializedName("description", alternate = ["Description"])
    val description: String? = null,

    @SerializedName("latitude", alternate = ["Latitude", "lat", "Lat"])
    val latitude: Double = 0.0,

    @SerializedName("longitude", alternate = ["Longitude", "lng", "Lng", "lon", "Lon"])
    val longitude: Double = 0.0,

    @SerializedName("capacityKwh", alternate = ["CapacityKwh", "capacity", "Capacity"])
    val capacityKwh: Double = 0.0,

    @SerializedName("availableBatteryStorageSlots", alternate = ["AvailableBatteryStorageSlots", "availableSlots", "AvailableSlots"])
    val availableBatteryStorageSlots: Int = 0,

    @SerializedName("schedule", alternate = ["Schedule"])
    val schedule: OperationalSchedule? = null,

    @SerializedName("status", alternate = ["Status"])
    val status: String? = null
) {
    /** Helper property returning human-readable station title */
    val displayName: String
        get() {
            if (name.isNotBlank()) return name
            if (stationId.isNotBlank()) return "Station $stationId"
            if (id.isNotBlank()) return "Station $id"
            return "Solar Microgrid Node"
        }
}

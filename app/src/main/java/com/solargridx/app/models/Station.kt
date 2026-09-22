package com.solargridx.app.models

/**
 * Station.kt
 * Purpose : Data class matching the SolarStation JSON response from the REST API.
 *           Used as the Gson deserialization target for Retrofit and as the
 *           domain model passed through the repository → ViewModel → Activity chain.
 * Author  : Member 2
 * Date    : 2026-09-21
 */

/** Represents a single day's operating window inside an OperationalSchedule. */
data class DailyHours(
    /** 0 = Sunday … 6 = Saturday, matching C# DayOfWeek. */
    val day: Int,
    val open: String,   // "HH:mm"
    val close: String,  // "HH:mm"
)

/** Holds the weekly operating schedule for a station node. */
data class OperationalSchedule(
    val timeZoneId: String? = null,
    val days: List<DailyHours> = emptyList(),
)

/** Top-level station response object from GET /api/stations and GET /api/stations/{id}. */
data class Station(
    val id: String,
    val stationId: String,
    val name: String,
    val description: String?,
    val latitude: Double,
    val longitude: Double,
    val capacityKwh: Double,
    val availableBatteryStorageSlots: Int,
    val schedule: OperationalSchedule,
    val status: String,   // "Active" or "Deactivated"
)

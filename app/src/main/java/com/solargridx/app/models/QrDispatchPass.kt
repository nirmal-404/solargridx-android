package com.solargridx.app.models

/**
 * QrDispatchPass.kt
 * Model representing a QR Dispatcher pass token generated for solar station energy dispatch.
 */
data class QrDispatchPass(
    val id: String,
    val reservationId: String,
    val stationId: String,
    val stationName: String,
    val energyKwh: Double,
    val batterySlotNumber: Int,
    val userEmail: String,
    val qrPayload: String,
    val status: String, // "READY", "DISPATCHED", "REDEEMED", "EXPIRED"
    val timestamp: String
)

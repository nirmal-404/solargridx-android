package com.solargridx.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.solargridx.app.databinding.ActivitySummaryBinding

class SummaryActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySummaryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySummaryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val operation = intent.getStringExtra("operation") ?: "Booking Confirmed"
        val reservationId = intent.getStringExtra("reservationId") ?: ""
        val stationId = intent.getStringExtra("stationId") ?: ""
        val capacity = intent.getDoubleExtra("capacity", 0.0)
        val startTime = intent.getStringExtra("startTime") ?: ""
        val endTime = intent.getStringExtra("endTime") ?: ""
        val status = intent.getStringExtra("status") ?: "Pending"

        binding.tvOperationTitle.text = operation
        binding.tvReservationId.text = "Reservation ID: $reservationId"
        binding.tvStationId.text = "Station ID: $stationId"
        binding.tvCapacity.text = "Requested Capacity: $capacity kWh"
        binding.tvTimeRange.text = "Time: ${startTime.take(16).replace("T", " ")} — ${endTime.take(16).replace("T", " ")}"
        binding.tvStatus.text = "Status: $status"

        binding.btnDone.setOnClickListener {
            finish()
        }
    }
}

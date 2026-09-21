package com.solargridx.app.ui

import android.R
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.solargridx.app.databinding.ActivityCreateBookingBinding
import com.solargridx.app.models.SlotResponse
import com.solargridx.app.models.StationResponse
import kotlinx.coroutines.launch

class CreateBookingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateBookingBinding
    private val viewModel: CreateBookingViewModel by viewModels()

    private var selectedStation: StationResponse? = null
    private var selectedSlot: SlotResponse? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateBookingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        observeViewModel()

        viewModel.loadStations()
    }

    private fun setupListeners() {
        binding.btnSubmit.setOnClickListener {
            val station = selectedStation
            val slot = selectedSlot
            val capacityStr = binding.etCapacity.text.toString().trim()
            val capacity = capacityStr.toDoubleOrNull()

            if (station == null) {
                Toast.makeText(this, "Please select a solar station", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (slot == null) {
                Toast.makeText(this, "Please select a booking slot", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (capacity == null || capacity <= 0.0) {
                binding.tilCapacity.error = "Enter a valid positive capacity"
                return@setOnClickListener
            } else if (capacity > slot.availableCapacity) {
                binding.tilCapacity.error = "Capacity exceeds slot available (${slot.availableCapacity} kWh)"
                return@setOnClickListener
            } else {
                binding.tilCapacity.error = null
            }

            viewModel.createBooking(station.stationId, slot.slotId, capacity)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.stations.collect { stationList ->
                        val adapter = ArrayAdapter(
                            this@CreateBookingActivity,
                            R.layout.simple_spinner_item,
                            stationList.map { "${it.name} (${it.stationId})" }
                        )
                        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
                        binding.spinnerStation.adapter = adapter

                        binding.spinnerStation.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                                if (position in stationList.indices) {
                                    selectedStation = stationList[position]
                                    viewModel.loadSlotsForStation(stationList[position].stationId)
                                }
                            }
                            override fun onNothingSelected(parent: AdapterView<*>?) {}
                        }
                    }
                }

                launch {
                    viewModel.slots.collect { slotList ->
                        val adapter = ArrayAdapter(
                            this@CreateBookingActivity,
                            R.layout.simple_spinner_item,
                            slotList.map { "${it.startTime.take(16).replace("T", " ")} — ${it.endTime.take(16).replace("T", " ")} (${it.availableCapacity} kWh)" }
                        )
                        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
                        binding.spinnerSlot.adapter = adapter

                        binding.spinnerSlot.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                                if (position in slotList.indices) {
                                    selectedSlot = slotList[position]
                                }
                            }
                            override fun onNothingSelected(parent: AdapterView<*>?) {}
                        }
                    }
                }

                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is CreateBookingUiState.Idle -> showLoading(false)
                            is CreateBookingUiState.Loading -> showLoading(true)
                            is CreateBookingUiState.Success -> {
                                showLoading(false)
                                val intent = Intent(this@CreateBookingActivity, SummaryActivity::class.java).apply {
                                    putExtra("operation", "Booking Created")
                                    putExtra("reservationId", state.reservation.reservationId)
                                    putExtra("stationId", state.reservation.stationId)
                                    putExtra("capacity", state.reservation.requestedCapacity)
                                    putExtra("startTime", state.reservation.scheduledStartTime)
                                    putExtra("endTime", state.reservation.scheduledEndTime)
                                    putExtra("status", state.reservation.status.name)
                                }
                                startActivity(intent)
                                finish()
                            }
                            is CreateBookingUiState.Error -> {
                                showLoading(false)
                                Toast.makeText(this@CreateBookingActivity, state.message, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnSubmit.isEnabled = !isLoading
    }
}

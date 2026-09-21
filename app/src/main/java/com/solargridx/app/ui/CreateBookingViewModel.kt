package com.solargridx.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solargridx.app.models.CreateReservationRequest
import com.solargridx.app.models.ReservationResponse
import com.solargridx.app.models.SlotResponse
import com.solargridx.app.models.StationResponse
import com.solargridx.app.repositories.ReservationRepository
import com.solargridx.app.repositories.StationSlotRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class CreateBookingUiState {
    object Idle : CreateBookingUiState()
    object Loading : CreateBookingUiState()
    data class Success(val reservation: ReservationResponse) : CreateBookingUiState()
    data class Error(val message: String) : CreateBookingUiState()
}

class CreateBookingViewModel(
    private val reservationRepo: ReservationRepository = ReservationRepository(),
    private val stationSlotRepo: StationSlotRepository = StationSlotRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<CreateBookingUiState>(CreateBookingUiState.Idle)
    val uiState: StateFlow<CreateBookingUiState> = _uiState.asStateFlow()

    private val _stations = MutableStateFlow<List<StationResponse>>(emptyList())
    val stations: StateFlow<List<StationResponse>> = _stations.asStateFlow()

    private val _slots = MutableStateFlow<List<SlotResponse>>(emptyList())
    val slots: StateFlow<List<SlotResponse>> = _slots.asStateFlow()

    fun loadStations() {
        viewModelScope.launch {
            val result = stationSlotRepo.getStations(includeInactive = false)
            if (result.isSuccess) {
                _stations.value = result.getOrDefault(emptyList())
            }
        }
    }

    fun loadSlotsForStation(stationId: String) {
        viewModelScope.launch {
            val result = stationSlotRepo.getSlotsByStation(stationId, includeInactive = false)
            if (result.isSuccess) {
                val availableSlots = result.getOrDefault(emptyList()).filter { it.availableCapacity > 0 }
                _slots.value = availableSlots
            }
        }
    }

    fun createBooking(stationId: String, slotId: String, requestedCapacity: Double) {
        viewModelScope.launch {
            _uiState.value = CreateBookingUiState.Loading
            val request = CreateReservationRequest(
                stationId = stationId,
                slotId = slotId,
                requestedCapacity = requestedCapacity
            )
            val result = reservationRepo.createReservation(request)
            if (result.isSuccess) {
                _uiState.value = CreateBookingUiState.Success(result.getOrThrow())
            } else {
                val msg = result.exceptionOrNull()?.message ?: "Failed to create reservation"
                _uiState.value = CreateBookingUiState.Error(msg)
            }
        }
    }
}

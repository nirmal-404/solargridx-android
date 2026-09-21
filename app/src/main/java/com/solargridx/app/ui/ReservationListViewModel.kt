package com.solargridx.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solargridx.app.models.ReservationResponse
import com.solargridx.app.models.UpdateReservationRequest
import com.solargridx.app.repositories.ReservationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ReservationListUiState {
    object Idle : ReservationListUiState()
    object Loading : ReservationListUiState()
    data class Success(val reservations: List<ReservationResponse>) : ReservationListUiState()
    data class ActionSuccess(val message: String) : ReservationListUiState()
    data class Error(val message: String) : ReservationListUiState()
}

class ReservationListViewModel(
    private val repository: ReservationRepository = ReservationRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReservationListUiState>(ReservationListUiState.Idle)
    val uiState: StateFlow<ReservationListUiState> = _uiState.asStateFlow()

    fun loadPendingQueue() {
        viewModelScope.launch {
            _uiState.value = ReservationListUiState.Loading
            val result = repository.getPendingReservations()
            if (result.isSuccess) {
                _uiState.value = ReservationListUiState.Success(result.getOrDefault(emptyList()))
            } else {
                val msg = result.exceptionOrNull()?.message ?: "Failed to load pending queue"
                _uiState.value = ReservationListUiState.Error(msg)
            }
        }
    }

    fun loadBookingHistory() {
        viewModelScope.launch {
            _uiState.value = ReservationListUiState.Loading
            val result = repository.getBookingHistory()
            if (result.isSuccess) {
                _uiState.value = ReservationListUiState.Success(result.getOrDefault(emptyList()))
            } else {
                val msg = result.exceptionOrNull()?.message ?: "Failed to load booking history"
                _uiState.value = ReservationListUiState.Error(msg)
            }
        }
    }

    fun approveReservation(reservationId: String) {
        viewModelScope.launch {
            _uiState.value = ReservationListUiState.Loading
            val result = repository.approveReservation(reservationId)
            if (result.isSuccess) {
                _uiState.value = ReservationListUiState.ActionSuccess("Reservation $reservationId approved successfully")
            } else {
                val msg = result.exceptionOrNull()?.message ?: "Approval failed"
                _uiState.value = ReservationListUiState.Error(msg)
            }
        }
    }

    fun rejectReservation(reservationId: String) {
        viewModelScope.launch {
            _uiState.value = ReservationListUiState.Loading
            val result = repository.rejectReservation(reservationId)
            if (result.isSuccess) {
                _uiState.value = ReservationListUiState.ActionSuccess("Reservation $reservationId rejected")
            } else {
                val msg = result.exceptionOrNull()?.message ?: "Rejection failed"
                _uiState.value = ReservationListUiState.Error(msg)
            }
        }
    }

    fun cancelReservation(reservationId: String) {
        viewModelScope.launch {
            _uiState.value = ReservationListUiState.Loading
            val result = repository.cancelReservation(reservationId)
            if (result.isSuccess) {
                _uiState.value = ReservationListUiState.ActionSuccess("Reservation $reservationId cancelled successfully")
            } else {
                val msg = result.exceptionOrNull()?.message ?: "Cancellation failed"
                _uiState.value = ReservationListUiState.Error(msg)
            }
        }
    }

    fun updateReservation(reservationId: String, stationId: String, slotId: String, capacity: Double) {
        viewModelScope.launch {
            _uiState.value = ReservationListUiState.Loading
            val request = UpdateReservationRequest(stationId, slotId, capacity)
            val result = repository.updateReservation(reservationId, request)
            if (result.isSuccess) {
                _uiState.value = ReservationListUiState.ActionSuccess("Reservation $reservationId updated successfully")
            } else {
                val msg = result.exceptionOrNull()?.message ?: "Update failed"
                _uiState.value = ReservationListUiState.Error(msg)
            }
        }
    }
}

package com.solargridx.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solargridx.app.models.DashboardSummaryResponse
import com.solargridx.app.models.ReservationResponse
import com.solargridx.app.repositories.ReservationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class DashboardUiState {
    object Idle : DashboardUiState()
    object Loading : DashboardUiState()
    data class Success(
        val summary: DashboardSummaryResponse,
        val recentReservations: List<ReservationResponse>
    ) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}

class DashboardViewModel(
    private val repository: ReservationRepository = ReservationRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Idle)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = DashboardUiState.Loading
            val summaryResult = repository.getDashboardSummary()
            val listResult = repository.searchReservations()

            if (summaryResult.isSuccess) {
                val summary = summaryResult.getOrDefault(DashboardSummaryResponse())
                val recentList = listResult.getOrDefault(emptyList()).take(5)
                _uiState.value = DashboardUiState.Success(summary, recentList)
            } else {
                val errorMsg = summaryResult.exceptionOrNull()?.message ?: "Failed to load dashboard metrics"
                _uiState.value = DashboardUiState.Error(errorMsg)
            }
        }
    }
}

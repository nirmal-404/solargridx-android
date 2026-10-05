package com.solargridx.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.solargridx.app.models.CreateStationRequest
import com.solargridx.app.models.OperationalSchedule
import com.solargridx.app.models.Station
import com.solargridx.app.models.UpdateStationRequest
import com.solargridx.app.models.UpdateStationScheduleRequest
import com.solargridx.app.repositories.StationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class NodeManagementViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = StationRepository(application)

    private val _stations = MutableStateFlow<List<Station>>(emptyList())
    val stations: StateFlow<List<Station>> = _stations

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private val _isError = MutableStateFlow(false)
    val isError: StateFlow<Boolean> = _isError

    // Loads all active and inactive nodes for the Backoffice management list.
    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _message.value = null
            repository.getManagedStations()
                .onSuccess { _stations.value = it }
                .onFailure { showError(it.message ?: "Could not load nodes.") }
            _isLoading.value = false
        }
    }

    // Creates a node through the API and refreshes the list after success.
    fun create(request: CreateStationRequest) = mutate("Node created successfully.") {
        repository.createStation(request).map { Unit }
    }

    // Updates mutable node details through the API and refreshes the list.
    fun update(stationId: String, request: UpdateStationRequest) = mutate("Node updated successfully.") {
        repository.updateStation(stationId, request).map { Unit }
    }

    // Replaces the weekly operating schedule through the API.
    fun updateSchedule(stationId: String, schedule: OperationalSchedule) =
        mutate("Node schedule updated successfully.") {
            repository.updateSchedule(stationId, UpdateStationScheduleRequest(schedule)).map { Unit }
        }

    // Deactivates a node; the server rejects this while active reservations exist.
    fun deactivate(stationId: String) = mutate("Node deactivated.") {
        repository.deactivateStation(stationId)
    }

    // Reactivates an inactive node through the API.
    fun reactivate(stationId: String) = mutate("Node reactivated.") {
        repository.reactivateStation(stationId)
    }

    // Runs one mutation and reloads server state so the UI never invents node status.
    private fun mutate(successMessage: String, operation: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            _isSaving.value = true
            _message.value = null
            operation()
                .onSuccess {
                    _isError.value = false
                    _message.value = successMessage
                    repository.getManagedStations().onSuccess { _stations.value = it }
                }
                .onFailure { showError(it.message ?: "Node operation failed.") }
            _isSaving.value = false
        }
    }

    // Publishes a server or transport error for the screen banner.
    private fun showError(message: String) {
        _isError.value = true
        _message.value = message
    }
}
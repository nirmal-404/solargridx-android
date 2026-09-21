package com.solargridx.app.ui

/**
 * MapsViewModel.kt
 * Purpose : ViewModel for MapsActivity. Fetches station data from StationRepository
 *           on a background coroutine (viewModelScope) and exposes immutable
 *           StateFlow streams to the Activity. The Activity only observes — it
 *           never makes network calls directly. This keeps the Activity thin and
 *           the logic unit-testable without a running Android environment.
 * Author  : Member 2
 * Date    : 2026-09-21
 */

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.solargridx.app.models.Station
import com.solargridx.app.repositories.StationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MapsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StationRepository(application)

    /** Backing field for the station list — private to prevent external mutation. */
    private val _stations = MutableStateFlow<List<Station>>(emptyList())

    /** Observed by MapsActivity to plot markers when new data arrives. */
    val stations: StateFlow<List<Station>> = _stations

    /** Non-null when a load or network error has occurred. */
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    /** True while a network request is in flight. */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        // Kick off the initial station load when the ViewModel is first created.
        loadStations()
    }

    /** Fetches active stations from the API and updates the observable state flows. */
    fun loadStations() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.getStations(includeInactive = false)
                .onSuccess { stationList ->
                    _stations.value = stationList
                }
                .onFailure { throwable ->
                    _error.value = throwable.message ?: "Unknown error loading stations."
                }

            _isLoading.value = false
        }
    }
}

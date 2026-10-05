package com.solargridx.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.solargridx.app.models.DashboardSummary
import com.solargridx.app.repositories.DashboardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DashboardRepository(application)

    private val _summary = MutableStateFlow<DashboardSummary?>(null)
    val summary: StateFlow<DashboardSummary?> = _summary.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadSummary()
    }

    fun loadSummary() {
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            val result = repository.getDashboardSummary()
            result.fold(
                onSuccess = { data ->
                    _summary.value = data
                    _isLoading.value = false
                },
                onFailure = { throwable ->
                    _isLoading.value = false
                    _error.value = throwable.localizedMessage ?: "Failed to fetch dashboard summary"
                }
            )
        }
    }
}

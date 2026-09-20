package com.solargridx.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solargridx.app.models.LoginRequest
import com.solargridx.app.models.User
import com.solargridx.app.repositories.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    data class Success(val user: User, val token: String) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

class LoginViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            val result = authRepository.login(LoginRequest(email, password))
            result.fold(
                onSuccess = { response ->
                    val user = response.user
                    val token = response.token
                    if (response.success && user != null && token != null) {
                        _uiState.value = LoginUiState.Success(user, token)
                    } else {
                        _uiState.value = LoginUiState.Error(response.message ?: "Authentication failed")
                    }
                },
                onFailure = { throwable ->
                    _uiState.value = LoginUiState.Error(throwable.localizedMessage ?: "Login error occurred")
                }
            )
        }
    }
}

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
    data class Success(val user: User?, val token: String) : LoginUiState()
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
            val request = LoginRequest(email = email, password = password)
            val result = authRepository.login(request)
            result.fold(
                onSuccess = { response ->
                    val token = response.token ?: ""
                    var user = response.user

                    if (token.isNotEmpty() && user == null) {
                        val userResult = authRepository.getCurrentUser(token)
                        user = userResult.getOrNull()
                    }

                    _uiState.value = LoginUiState.Success(user, token)
                },
                onFailure = { throwable ->
                    _uiState.value = LoginUiState.Error(
                        throwable.localizedMessage ?: "Unable to connect to C# backend server"
                    )
                }
            )
        }
    }
}

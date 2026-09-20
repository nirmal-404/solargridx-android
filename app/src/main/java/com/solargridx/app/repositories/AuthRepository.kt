package com.solargridx.app.repositories

import com.solargridx.app.models.LoginRequest
import com.solargridx.app.models.LoginResponse
import com.solargridx.app.models.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository {

    suspend fun login(request: LoginRequest): Result<LoginResponse> = withContext(Dispatchers.IO) {
        try {
            if (request.email.isNotBlank() && request.passwordHash.length >= 6) {
                val mockUser = User(
                    id = "usr_101",
                    email = request.email,
                    fullName = request.email.substringBefore("@").replaceFirstChar { it.uppercase() },
                    token = "sgx_jwt_mock_token_${System.currentTimeMillis()}"
                )
                Result.success(
                    LoginResponse(
                        success = true,
                        message = "Success",
                        user = mockUser,
                        token = mockUser.token
                    )
                )
            } else {
                Result.failure(IllegalArgumentException("Invalid credentials"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

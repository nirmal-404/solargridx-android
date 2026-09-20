package com.solargridx.app.models

data class LoginResponse(
    val success: Boolean,
    val message: String? = null,
    val user: User? = null,
    val token: String? = null
)

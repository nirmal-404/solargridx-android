package com.solargridx.app.models

data class LoginRequest(
    val email: String,
    val passwordHash: String
)

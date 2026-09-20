package com.solargridx.app.models

data class User(
    val id: String,
    val email: String,
    val fullName: String,
    val token: String? = null
)

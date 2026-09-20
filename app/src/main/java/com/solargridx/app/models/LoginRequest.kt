package com.solargridx.app.models

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("email")
    val email: String,

    @SerializedName("password")
    val password: String,

    @SerializedName("nic")
    val nic: String = email,

    @SerializedName("username")
    val username: String = email
)

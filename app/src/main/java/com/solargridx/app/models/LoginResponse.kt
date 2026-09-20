package com.solargridx.app.models

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("success", alternate = ["isSuccess", "Success"])
    val success: Boolean = true,

    @SerializedName("message", alternate = ["Message", "error"])
    val message: String? = null,

    @SerializedName("token", alternate = ["Token", "accessToken", "jwt"])
    val token: String? = null,

    @SerializedName("user", alternate = ["User", "data"])
    val user: User? = null
)

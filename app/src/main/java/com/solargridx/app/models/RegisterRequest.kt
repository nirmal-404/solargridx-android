package com.solargridx.app.models

import com.google.gson.annotations.SerializedName

data class RegisterRequest(
    @SerializedName("nic") val nic: String,
    @SerializedName("firstName") val firstName: String,
    @SerializedName("lastName") val lastName: String,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("address") val address: String? = null
)

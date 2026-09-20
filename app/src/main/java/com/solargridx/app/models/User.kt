package com.solargridx.app.models

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("id", alternate = ["Id", "_id"])
    val id: String? = null,

    @SerializedName("nic", alternate = ["Nic", "NIC"])
    val nic: String? = null,

    @SerializedName("email", alternate = ["Email"])
    val email: String? = null,

    @SerializedName("fullName", alternate = ["FullName", "name", "Name", "username"])
    val fullName: String? = null,

    @SerializedName("role", alternate = ["Role", "userType"])
    val role: String? = null,

    @SerializedName("token", alternate = ["Token", "accessToken"])
    val token: String? = null
)

package com.solargridx.app.models

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("id", alternate = ["Id", "_id"])
    val id: String? = null,

    @SerializedName("nic", alternate = ["Nic", "NIC"])
    val nic: String? = null,

    @SerializedName("email", alternate = ["Email"])
    val email: String? = null,

    @SerializedName("firstName", alternate = ["FirstName"])
    val firstName: String? = null,

    @SerializedName("lastName", alternate = ["LastName"])
    val lastName: String? = null,

    @SerializedName("fullName", alternate = ["FullName", "name", "Name", "username"])
    val fullName: String? = null,

    @SerializedName("phone", alternate = ["Phone", "phoneNumber"])
    val phone: String? = null,

    @SerializedName("address", alternate = ["Address"])
    val address: String? = null,

    @SerializedName("role", alternate = ["Role", "userType"])
    val role: String? = null,

    @SerializedName("accountStatus", alternate = ["AccountStatus", "status"])
    val accountStatus: String? = null,

    @SerializedName("token", alternate = ["Token", "accessToken"])
    val token: String? = null
) {
    val displayName: String
        get() = when {
            !fullName.isNullOrBlank() -> fullName
            !firstName.isNullOrBlank() || !lastName.isNullOrBlank() -> "${firstName.orEmpty()} ${lastName.orEmpty()}".trim()
            !email.isNullOrBlank() -> email
            else -> "User"
        }
}


package com.solargridx.app.models

import com.google.gson.annotations.SerializedName

data class UpdateProsumerRequest(
    @SerializedName("firstName") val firstName: String,
    @SerializedName("lastName") val lastName: String,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("address") val address: String? = null
)

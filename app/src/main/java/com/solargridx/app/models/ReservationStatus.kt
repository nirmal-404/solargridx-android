package com.solargridx.app.models

import com.google.gson.annotations.SerializedName

enum class ReservationStatus {
    @SerializedName("Pending")
    Pending,

    @SerializedName("Approved")
    Approved,

    @SerializedName("Rejected")
    Rejected,

    @SerializedName("Cancelled")
    Cancelled,

    @SerializedName("Completed")
    Completed,

    @SerializedName("Expired")
    Expired
}

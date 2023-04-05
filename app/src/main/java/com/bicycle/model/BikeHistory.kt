package com.bicycle.model

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson

data class BikeHistory(
    val bikeId: Int,
    val history: List<BikeStatusChange>
)

data class BikeStatusChange(
    val bikeId: Int,
    val bikeName: String,
    val timestamp: Long,
    val fromStatus: StatusBike,
    val toStatus: StatusBike
)

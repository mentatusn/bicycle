package com.bicycle.model


data class BikeStatusChange(
    val bikeId: Int,
    val bikeName: String,
    val timestamp: Long,
    val fromStatus: StatusBike,
    val toStatus: StatusBike
)

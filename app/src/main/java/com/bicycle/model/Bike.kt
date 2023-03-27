package com.bicycle.model

data class Bike(
    val id: Int,
    val name: String,
    val price: String,
    val rentDuration: Long,
    var startTime: Long,
    var endTime: Long,
    val color: Int
)
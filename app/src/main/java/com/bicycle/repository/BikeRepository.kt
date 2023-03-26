package com.bicycle.repository

import android.graphics.Color
import com.bicycle.model.Bike

class BikeRepository {
    suspend fun getBikes(): List<Bike> {
        // Здесь должна быть реализация получения списка велосипедов
        // Например, из локальной базы данных, с сервера или из статических данных
        return fillBikesData()
    }

    private fun fillBikesData() = List(24) { i ->
        Bike(
            name = (i + 1).toString().padStart(3, '0'),
            price = "1000тг.",
            rentDuration = "20мин.",
            startTime = "${(9..17).random()}:00",
            endTime = "${(10..22).random()}:00",
            randomColors.random()
        )
    }.toMutableList()

    val randomColors = listOf<Int>(
        0xFF9ad2ae.toInt(),
        //0xFFcbe6d3.toInt(),
        0xFFf8a98e.toInt(),
        0xFFfcd2c0.toInt(),
        0xFFf5adce.toInt(),
        0xFFcb90ac.toInt(),
        0xFFf05972.toInt(),
        0xFFf38480.toInt(),
        0xFF00aeef.toInt(),
        0xFFc656a0.toInt(),
        0xFFfeca0a.toInt(),
        0xFF00a54f.toInt() )
}
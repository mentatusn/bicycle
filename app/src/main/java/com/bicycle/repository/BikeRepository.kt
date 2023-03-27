package com.bicycle.repository

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.work.*
import com.bicycle.MyApplication
import com.bicycle.model.Bike
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

class BikeRepository() {
    private val sharedPreferences: SharedPreferences =
        MyApplication.applicationContext().getSharedPreferences("bikes_data", Context.MODE_PRIVATE)

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    private val _bikes = MutableLiveData<List<Bike>>()
    val bikes: LiveData<List<Bike>> get() = _bikes

    init {
        loadBikes()
        startGlobalTimer()
    }

    fun getBikes(): List<Bike> = _bikes.value ?: emptyList()

    private fun loadBikes() {
        if (sharedPreferences.getString("bikes", null) == null) {
            saveBikesData(fillBikesData())
        }
        _bikes.value = loadBikesData()
    }


    fun updateBike(bike: Bike, onBikeUpdated: () -> Unit) {
        val bikes = loadBikesData().toMutableList()
        val index = bikes.indexOfFirst { it.name == bike.name }
        if (index != -1) {
            bikes[index] = bike
            saveBikesData(bikes)
            onBikeUpdated()
        }
    }

    private fun startGlobalTimer() {
        scope.launch {
            while (isActive) {
                val bikes = loadBikesData()
                val updatedBikes = bikes.map { bike ->

                    if (bike.endTime != 0L) {
                        Log.d("bike.endTime","${bike.endTime}")
                        bike.endTime =bike.startTime+bike.rentDuration*10*1000 - System.currentTimeMillis()
                        if (bike.endTime <= 0) {
                            bike.copy(endTime = 0, color = bike.color)
                        } else {
                            bike
                        }
                    } else {
                        bike
                    }
                }
                saveBikesData(updatedBikes)
                _bikes.value = updatedBikes
                delay(1000)
            }
        }
    }

    private fun saveBikesData(bikes: List<Bike>) {
        val editor = sharedPreferences.edit()
        val json = Gson().toJson(bikes)
        editor.putString("bikes", json)
        editor.apply()
    }

    private fun loadBikesData(): List<Bike> {
        val json = sharedPreferences.getString("bikes", null)
        val type = object : TypeToken<List<Bike>>() {}.type
        return Gson().fromJson(json, type)
    }

    private fun fillBikesData() = List(24) { i ->
        Bike(
            id = i,
            name = (i + 1).toString().padStart(3, '0'),
            price = "1000тг.",
            rentDuration = 1,
            startTime = 0,
            endTime = 0,
            color = randomColors.random()
        )
    }.toMutableList()


}

val randomColors = listOf(
    0xFF9ad2ae.toInt(),
    0xFFf8a98e.toInt(),
    0xFFfcd2c0.toInt(),
    0xFFf5adce.toInt(),
    0xFFcb90ac.toInt(),
    0xFFf05972.toInt(),
    0xFFf38480.toInt(),
    0xFF00aeef.toInt(),
    0xFFc656a0.toInt(),
    0xFFfeca0a.toInt(),
    0xFF00a54f.toInt()
)


//0xFFcbe6d3.toInt(),
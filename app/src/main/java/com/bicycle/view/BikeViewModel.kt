package com.bicycle.view

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

import com.bicycle.model.Bike
import com.bicycle.repository.BikeRepository
import kotlinx.coroutines.*

class BikeViewModel(private val repository: BikeRepository = BikeRepository()) : ViewModel() {
    val bikes = repository.bikes

    fun pressBike(bike:Bike){
        CoroutineScope(Job()).launch {
            bike.startTime = System.currentTimeMillis()
            bike.endTime = bike.rentDuration*60*1000
            repository.updateBike(bike){}
        }
    }
}
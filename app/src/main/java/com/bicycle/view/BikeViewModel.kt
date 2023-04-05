package com.bicycle.view

import androidx.lifecycle.ViewModel
import com.bicycle.model.Bike
import com.bicycle.model.StatusBike
import com.bicycle.repository.BikeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class BikeViewModel(private val repository: BikeRepository = BikeRepository()) : ViewModel() {
    val bikes = repository.bikes
    val appStateLiveData = repository.appStateLiveData

    fun pressBike(bike: Bike) {
        CoroutineScope(Job()).launch {
            var text = ""
            when (bike.status) {
                StatusBike.IDLE -> {
                    bike.startTime = System.currentTimeMillis()
                    bike.endTime = bike.rentDuration * 60 * 1000
                    bike.status = StatusBike.ACTIVE
                    text = "Арендован"
                }
                StatusBike.ACTIVE -> {
                    if ((System.currentTimeMillis() - bike.startTime) / 1000 < 60) {
                        bike.endTime = 1
                        bike.status = StatusBike.CANCELED
                        text = "Отмена"
                    } else {
                        bike.endTime = 1
                        bike.status = StatusBike.IDLE
                        text = "Закрыли"
                    }
                }
                StatusBike.WAIT_FOR_CANCEL -> {
                    bike.endTime = 1
                    bike.status = StatusBike.IDLE
                    text = "Закрыли"
                }
                StatusBike.CANCELED -> {
                    bike.startTime = System.currentTimeMillis()
                    bike.status = StatusBike.ACTIVE
                    text = "Арендован"
                }
            }
            repository.updateBike(bike, text){}
        }
    }

}
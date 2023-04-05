package com.bicycle.view

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bicycle.R
import com.bicycle.model.Bike
import com.bicycle.model.StatusBike
import com.bicycle.repository.BikeRepository
import kotlinx.coroutines.launch

class BikeViewModel(
    private val context: Application,
    private val repository: BikeRepository = BikeRepository()
) : AndroidViewModel(context) {
    val bikes = repository.bikes
    val appStateLiveData = repository.appStateLiveData

    fun pressBike(bike: Bike) {
        viewModelScope.launch {
            var text = ""
            val currentTime = System.currentTimeMillis()
            when (bike.status) {
                StatusBike.IDLE -> {
                    bike.startTime = currentTime
                    bike.endTime = bike.rentDuration * 60 * 1000
                    bike.status = StatusBike.ACTIVE
                    text = context.getString(R.string.bike_rented)
                }
                StatusBike.ACTIVE -> {
                    if ((currentTime - bike.startTime) / 1000 < 60) {
                        bike.endTime = 1
                        bike.status = StatusBike.CANCELED
                        text = context.getString(R.string.bike_cancelled)
                    } else {
                        bike.endTime = 1
                        bike.status = StatusBike.IDLE
                        text = context.getString(R.string.bike_closed)
                    }
                }
                StatusBike.WAIT_FOR_CANCEL -> {
                    bike.endTime = 1
                    bike.status = StatusBike.IDLE
                    text = context.getString(R.string.bike_closed)
                }
                StatusBike.CANCELED -> {
                    bike.startTime = currentTime
                    bike.status = StatusBike.ACTIVE
                    text = context.getString(R.string.bike_rented)
                }
            }
            repository.updateBike(bike, text) {}
        }
    }

}
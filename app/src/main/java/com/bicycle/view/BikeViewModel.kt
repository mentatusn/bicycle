package com.bicycle.view

import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bicycle.MainActivity

import com.bicycle.model.Bike
import com.bicycle.model.StatusBike
import com.bicycle.repository.BikeRepository
import kotlinx.coroutines.*

class BikeViewModel(private val repository: BikeRepository = BikeRepository()) : ViewModel() {
    val bikes = repository.bikes

    fun pressBike(bike: Bike) {
        CoroutineScope(Job()).launch {
            when (bike.status) {
                StatusBike.IDLE -> {
                    bike.startTime = System.currentTimeMillis()
                    bike.endTime = bike.rentDuration * 60 * 1000
                    bike.status = StatusBike.ACTIVE
                    val text = "Арендован"
                    toast(text, bike)
                }
                StatusBike.ACTIVE -> {
                    if ((System.currentTimeMillis() - bike.startTime) / 1000 < 60) {
                        bike.endTime = 1
                        bike.status = StatusBike.CANCELED
                        val text = "Отмена"
                        toast(text, bike)
                    }else{
                        bike.endTime = 1
                        bike.status = StatusBike.IDLE
                        val text = "Закрыли"
                        toast(text, bike)
                    }
                }
                StatusBike.WAIT_FOR_CANCEL -> {
                    bike.endTime = 1
                    bike.status = StatusBike.IDLE
                    val text = "Закрыли"
                    toast(text, bike)
                }
                StatusBike.CANCELED -> {
                    bike.startTime = System.currentTimeMillis()
                    bike.status = StatusBike.ACTIVE
                    val text = "Арендован"
                    toast(text, bike)
                }
            }

            val f = bike
            val i = f.id
            repository.updateBike(bike) {}
        }
    }

    private fun toast(text: String, bike: Bike) {
        MainActivity.instance.runOnUiThread {
            Toast.makeText(
                MainActivity.instance,
                "$text ${bike.name}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
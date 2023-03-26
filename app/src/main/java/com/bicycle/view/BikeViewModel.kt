package com.bicycle.view

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bicycle.model.Bike
import com.bicycle.repository.BikeRepository
import kotlinx.coroutines.launch

class BikeViewModel(private val repository: BikeRepository = BikeRepository()) : ViewModel() {
    val bikes = MutableLiveData<List<Bike>>()
    val selectedBike = MutableLiveData<Bike?>()

    init {
        loadBikes()
    }

    private fun loadBikes() {
        viewModelScope.launch {
            bikes.value = repository.getBikes()
        }
    }

    fun onBikeSelected(bike: Bike) {
        selectedBike.value = bike
    }

    fun onTimerFinished() {
        selectedBike.value = null
    }
}
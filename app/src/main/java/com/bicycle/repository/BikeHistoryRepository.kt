package com.bicycle.repository

import android.content.Context
import android.content.SharedPreferences
import com.bicycle.MyApplication
import com.bicycle.model.BikeStatusChange
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class BikeHistoryRepository() {
    private val sharedPreferences: SharedPreferences =
        MyApplication.applicationContext().getSharedPreferences("bikes_history_data", Context.MODE_PRIVATE)

    private val gson = Gson()


    fun getLastStatusChangeForBike(bikeId: Int): BikeStatusChange? {
        val changes = loadStatusChanges()
        return changes.filter { it.bikeId == bikeId }.lastOrNull()
    }

    fun addStatusChange(change: BikeStatusChange) {
        val changes = loadStatusChanges()
        changes.add(change)
        saveStatusChanges(changes)
    }

    fun getStatusChangesForBike(bikeId: Int): List<BikeStatusChange> {
        val changes = loadStatusChanges()
        return changes.filter { it.bikeId == bikeId }
    }

    fun loadStatusChanges(): MutableList<BikeStatusChange> {
        val json = sharedPreferences.getString("status_changes", "[]")
        val type = object : TypeToken<MutableList<BikeStatusChange>>() {}.type
        return gson.fromJson(json, type)
    }

    private fun saveStatusChanges(changes: MutableList<BikeStatusChange>) {
        val editor = sharedPreferences.edit()
        val json = gson.toJson(changes)
        editor.putString("status_changes", json)
        editor.apply()
    }
}

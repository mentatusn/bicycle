package com.bicycle.repository

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.util.TimeUtils.formatDuration
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.bicycle.MainActivity
import com.bicycle.MyApplication
import com.bicycle.model.*
import com.bicycle.util.Const
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.ValueRange
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
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
        if (sharedPreferences.getString(Const.sharedKey, null) == null) {
            saveBikesData(fillBikesData())
        }
        _bikes.value = loadBikesData()
    }


    fun updateSheetStart(bike: Bike) {

    }

    fun updateBike(bike: Bike, onBikeUpdated: () -> Unit) {
        val bikes = loadBikesData().toMutableList()
        val index = bikes.indexOfFirst { it.id == bike.id }
        if (bikes.size > 0 && index != -1) {
            bikes[index] = bike
            saveBikesData(bikes)
            onBikeUpdated()
            Thread {
                sliceHistory(bike)
            }.start()
        }
    }

    private fun sliceHistory(bike: Bike) {
        val bikeHistoryRepository = BikeHistoryRepository()
        val currentTimestamp = System.currentTimeMillis()
        var lastStatusChange = bikeHistoryRepository.getLastStatusChangeForBike(bike.id)
        if (lastStatusChange == null || lastStatusChange.toStatus != bike.status) {
            val statusChange =
                BikeStatusChange(
                    bike.id,
                    bike.name,
                    currentTimestamp,
                    lastStatusChange?.toStatus ?: StatusBike.IDLE,
                    bike.status
                )
            bikeHistoryRepository.addStatusChange(statusChange)
            if(lastStatusChange?.toStatus!=StatusBike.ACTIVE&&lastStatusChange?.toStatus!=StatusBike.WAIT_FOR_CANCEL){
                return
            }
            val today = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())


            val values = listOf(
                statusChange.bikeName,
                "Из \"${StatusBikeMapper().toString(lastStatusChange.toStatus)}\" ${formatTimestamp(lastStatusChange.timestamp)}",
                "В \"${StatusBikeMapper().toString(statusChange.toStatus)}\" ${formatTimestamp(statusChange.timestamp)}",
                transitions[lastStatusChange.toStatus]?.get(statusChange.toStatus) ?: "???",
                "Длительность ${formatDuration(statusChange.timestamp-lastStatusChange.timestamp)}"
            )
            var values2 = mutableListOf<List<String>>()

            getBikes().forEach { itBike->
                bikeHistoryRepository.loadStatusChanges().filter {
                    (it.bikeId == itBike.id)&&(it.timestamp > (System.currentTimeMillis() - 24 * 60*60 * 1000))}
                    .forEach {statusChange1->
                        //lastStatusChange!!.toStatus!=StatusBike.CANCELED&&
                        //&&statusChange1?.toStatus!=StatusBike.WAIT_FOR_CANCEL
                        if( statusChange1?.toStatus!=StatusBike.ACTIVE)
                            values2.add(
                                listOf(
                                    statusChange1.bikeName,
                                    "Из \"${StatusBikeMapper().toString(lastStatusChange!!.toStatus)} ${formatTimestamp(lastStatusChange!!.timestamp)}\"",
                                    "В \"${StatusBikeMapper().toString(statusChange1.toStatus)} ${formatTimestamp(statusChange1.timestamp)}\"",
                                    transitions[lastStatusChange!!.toStatus]?.get(statusChange1.toStatus) ?: "???",
                                    "Длительность ${formatDuration(statusChange1.timestamp-lastStatusChange!!.timestamp)}",
                                    formatTimestamp(statusChange1.timestamp)
                                ))
                        lastStatusChange = statusChange1
                    }
            }

            values2.sortBy { it[5] }
            values2 = values2.map {
                listOf(it[0],it[1],it[2],it[3],it[4])
            }.toMutableList()
            val httpTransport = NetHttpTransport()
            val jsonFactory: JsonFactory = JacksonFactory.getDefaultInstance()
            val serviceAccountPath =
                MyApplication.applicationContext().assets.open("augmented-ward-357019-f7f6d5778329.json")
            //val credential = GoogleCredential.fromStream(FileInputStream(serviceAccountPath), httpTransport, jsonFactory).createScoped(listOf("https://www.googleapis.com/auth/spreadsheets"))
            val credential =
                GoogleCredential.fromStream(serviceAccountPath, httpTransport, jsonFactory)
                    .createScoped(listOf("https://www.googleapis.com/auth/spreadsheets"))
            val sheetsService = Sheets.Builder(httpTransport, jsonFactory, credential)
                .setApplicationName("My App Name")
                .build()


            try {
                if (values2.isNotEmpty())
                    writeDataAndClear(
                        sheetsService,
                        Const.sheetId,
                        "${Const.sheet3}!A1:E${values2.size + 1000}", values2
                    )
                val rows = sheetsService.spreadsheets().values()
                    .get(Const.sheetId, "${Const.sheet2}!A:A")
                    .execute()
                    .getValues()
                val lastRowIndex = if (rows != null) rows.size else 0
                val range = "${Const.sheet2}!A${lastRowIndex + 1}:E${lastRowIndex + values.size}"
                if (values.isNotEmpty())
                    appendData(
                        sheetsService,
                        Const.sheetId,
                        range, (listOf(values))
                    )
            } catch (e: GoogleJsonResponseException) {
                Handler(Looper.getMainLooper()).post {
                    val errorCode = e.details.errors[0].reason
                    val errorMessage = e.details.errors[0].message
                    val dialogBuilder = AlertDialog.Builder(MainActivity.instance)
                        .setTitle("Error: $errorCode")
                        .setMessage(errorMessage)
                        .setPositiveButton("OK") { _, _ -> }
                    val dialog = dialogBuilder.create()
                    dialog.show()
                }
            }

        }
    }

    private fun startGlobalTimer() {
        scope.launch(Dispatchers.IO) {
            var count = 0
            var down = Const.holderGoogle
            while (isActive) {
                val bikes = loadBikesData()
                val updatedBikes = bikes.map { bike ->

                    if (bike.endTime == 1L && bike.status == StatusBike.IDLE) {
                        bike.endTime = 0L
                    }
                    if (bike.status == StatusBike.CANCELED) {
                        bike.status = StatusBike.IDLE
                    }

                    if (bike.status == StatusBike.ACTIVE || bike.status == StatusBike.WAIT_FOR_CANCEL) {
                        Log.d("bike.endTime", "${bike.endTime}")
                        bike.endTime =
                            bike.startTime + bike.rentDuration * 60 * 1000 - System.currentTimeMillis()
                        if (bike.endTime <= Const.alarmTimerHolder) {
                            if (bike.status != StatusBike.WAIT_FOR_CANCEL) {
                                bike.status = StatusBike.WAIT_FOR_CANCEL
                                sliceHistory(bike)
                            }
                        }
                        if (bike.endTime <= 0) {
                            bike.endTime = 0
                        }
                        bike
                    } else {
                        bike
                    }
                }
                saveBikesData(updatedBikes)
                _bikes.postValue(updatedBikes)

                try {
                    Log.d(
                        "mylogs",
                        "old ${count} ${bikes.filter { it.status == StatusBike.ACTIVE || it.status == StatusBike.WAIT_FOR_CANCEL }.size}"
                    )
                    val delete = count != bikes.filter { it.status == StatusBike.ACTIVE || it.status == StatusBike.WAIT_FOR_CANCEL }.size
                    if(delete||(--down<0)){
                        sliceCurrentActivity(
                            bikes,delete
                        )
                        down=Const.holderGoogle
                    }
                } catch (e: GoogleJsonResponseException) {

                    Handler(Looper.getMainLooper()).post {
                        val errorCode = e.details.errors[0].reason
                        val errorMessage = e.details.errors[0].message
                        val dialogBuilder = AlertDialog.Builder(MainActivity.instance)
                            .setTitle("Error: $errorCode")
                            .setMessage(errorMessage)
                            .setPositiveButton("OK") { _, _ -> }
                        val dialog = dialogBuilder.create()
                        dialog.show()
                    }
                    delay(30000) // Перенести работу корутины на минуту
                } finally {
                    Log.d("mylogs", "${count} new1")
                    count =
                        bikes.filter { it.status == StatusBike.ACTIVE || it.status == StatusBike.WAIT_FOR_CANCEL }.size
                    Log.d("mylogs", "${count} new2")
                    delay(1000)
                }

            }
        }
    }

    suspend private fun sliceCurrentActivity(
        bikes: List<Bike>,
        delete: Boolean
    ) {


        val values =
            bikes.filter { it.status == StatusBike.ACTIVE || it.status == StatusBike.WAIT_FOR_CANCEL }
                .map { bike ->
                    val start = formatTimestamp(bike.startTime)
                    val end = formatTimestamp(bike.startTime + bike.rentDuration * 60 * 1000)
                    val remaining = formatDuration(bike.endTime)
                    listOf(bike.name, start, end, remaining)
                }
        val httpTransport = NetHttpTransport()
        val jsonFactory: JsonFactory = JacksonFactory.getDefaultInstance()
        //val serviceAccountPath = "path/to/your-service-account-key.json"
        //val serviceAccountPath = "app/src/main/assets/augmented-ward-357019-f7f6d5778329.json"
        val serviceAccountPath =
            MyApplication.applicationContext().assets.open("augmented-ward-357019-f7f6d5778329.json")
        //val credential = GoogleCredential.fromStream(FileInputStream(serviceAccountPath), httpTransport, jsonFactory).createScoped(listOf("https://www.googleapis.com/auth/spreadsheets"))
        val credential =
            GoogleCredential.fromStream(serviceAccountPath, httpTransport, jsonFactory)
                .createScoped(listOf("https://www.googleapis.com/auth/spreadsheets"))
        val sheetsService = Sheets.Builder(httpTransport, jsonFactory, credential)
            .setApplicationName("My App Name")
            .build()
        if (values.isNotEmpty() || delete) {
            if (delete) {
                Log.d("mylogs", "delete")

                writeDataAndClear(
                    sheetsService,
                    Const.sheetId,
                    "${Const.sheet1}!A1:D${values.size + 1000}",
                    values
                )
            } else {
                Log.d("mylogs", "undelete")
                writeData(
                    sheetsService,
                    Const.sheetId,
                    "${Const.sheet1}!A1:D${values.size + 1}",
                    values
                )
            }
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        val date = Date(timestamp)
        val format = SimpleDateFormat("dd.MM HH:mm:ss")
        return format.format(date)
    }

    private fun formatDuration(timestamp: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(timestamp)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timestamp)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timestamp - TimeUnit.MINUTES.toMillis(minutes))
        return String.format("%02d:%02d:%02d",hours, minutes, seconds)
    }

    private fun saveBikesData(bikes: List<Bike>) {
        val editor = sharedPreferences.edit()
        val json = Gson().toJson(bikes)
        editor.putString(Const.sharedKey, json)
        editor.apply()
    }

    private fun loadBikesData(): List<Bike> {
        val json = sharedPreferences.getString(Const.sharedKey, null)
        val type = object : TypeToken<List<Bike>>() {}.type
        return Gson().fromJson(json, type)
    }

    private fun fillBikesData() = List(24) { i ->
        Bike(
            id = i,
            name = (i + 1).toString().padStart(3, '0'),
            price = Const.price,
            rentDuration = Const.rentDuration,
            startTime = 0,
            endTime = 0,
            color = randomColors.random(),
            status = StatusBike.IDLE
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
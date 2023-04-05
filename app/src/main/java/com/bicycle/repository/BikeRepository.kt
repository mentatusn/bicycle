package com.bicycle.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.bicycle.MyApplication
import com.bicycle.model.*
import com.bicycle.util.Const
import com.bicycle.util.randomColors
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class BikeRepository {
    private val sharedPreferences: SharedPreferences =
        MyApplication.applicationContext().getSharedPreferences("bikes_data", Context.MODE_PRIVATE)

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    private val _bikes = MutableLiveData<List<Bike>>()
    private val _appStateLiveData = MutableLiveData<AppState>()
    val bikes: LiveData<List<Bike>> get() = _bikes
    val appStateLiveData: LiveData<AppState> get() = _appStateLiveData

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


    /**
     * Updates the bike information and saves the updated data.
     *
     * @param bike The bike object with updated information.
     * @param text The text to be displayed on success.
     * @param onBikeUpdated A function to be called when the bike is successfully updated.
     */
    fun updateBike(bike: Bike, text: String = "", onBikeUpdated: () -> Unit) {
        val bikes = loadBikesData().toMutableList()
        val index = bikes.indexOfFirst { it.id == bike.id }
        if (bikes.size > 0 && index != -1) {
            bikes[index] = bike
            saveBikesData(bikes)
            Thread {
                try {
                    processBikeHistory(bike)
                    _appStateLiveData.postValue(AppState.Success(text))
                    onBikeUpdated()
                } catch (e: GoogleJsonResponseException) {
                    _appStateLiveData.postValue(AppState.Error(e.details.errors[0].message))
                }
            }.start()
        }
    }

    /**
     * Processes the bike's history by adding a new status change if necessary and writing the report to Google Sheets.
     *
     * @param bike The bike object to process.
     */
    private fun processBikeHistory(bike: Bike) {
        val bikeHistoryRepository = BikeHistoryRepository()

        val newStatusChange = addNewStatusChangeIfNeeded(bike, bikeHistoryRepository)
        if (newStatusChange != null) {
            val statusChangesReport = prepareStatusChangeReport(bikeHistoryRepository)
            writeToSheets(statusChangesReport)
        }
    }

    /**
     * Adds a new status change to the bike's history if the bike's status has changed.
     *
     * @param bike The bike object to check for status changes.
     * @param bikeHistoryRepository The BikeHistoryRepository instance to interact with bike history data.
     * @return The new BikeStatusChange object if a new status change was added, otherwise null.
     */
    private fun addNewStatusChangeIfNeeded(
        bike: Bike,
        bikeHistoryRepository: BikeHistoryRepository
    ): BikeStatusChange? {
        val currentTimestamp = System.currentTimeMillis()
        val lastStatusChange = bikeHistoryRepository.getLastStatusChangeForBike(bike.id)

        if (lastStatusChange == null || lastStatusChange.toStatus != bike.status) {
            val statusChange = BikeStatusChange(
                bike.id,
                bike.name,
                currentTimestamp,
                lastStatusChange?.toStatus ?: StatusBike.IDLE,
                bike.status
            )
            bikeHistoryRepository.addStatusChange(statusChange)
            return statusChange
        }

        return null
    }

    /**
     * Prepares a report of recent status changes for all bikes in the repository.
     *
     * @param bikeHistoryRepository The repository containing bike history data.
     * @return A list of rows representing status change information, where each row is a list of strings.
     */
    private fun prepareStatusChangeReport(bikeHistoryRepository: BikeHistoryRepository): List<List<String>> {
        val bikes = getBikes()
        val statusChangeRows = mutableListOf<List<String>>()

        bikes.forEach { bike ->
            val recentStatusChanges = bikeHistoryRepository.loadStatusChanges().filter {
                (it.bikeId == bike.id) && (it.timestamp > (System.currentTimeMillis() - 24 * 60 * 60 * 1000))
            }

            var previousStatusChange: BikeStatusChange? = null
            recentStatusChanges.forEach { currentStatusChange ->
                if (currentStatusChange.toStatus != StatusBike.ACTIVE && previousStatusChange != null) {
                    previousStatusChange?.let {
                        statusChangeRows.add(createStatusChangeRow(it, currentStatusChange))
                    }
                }
                previousStatusChange = currentStatusChange
            }
        }

        statusChangeRows.sortBy { it[5] }
        return statusChangeRows.map { listOf(it[0], it[1], it[2], it[3], it[4]) }
    }

    /**
     * Creates a row representing a status change event for a bike.
     *
     * @param lastStatusChange The previous status change event of the bike.
     * @param statusChange The current status change event of the bike.
     * @return A list of strings representing the details of the status change event.
     */
    private fun createStatusChangeRow(
        lastStatusChange: BikeStatusChange,
        statusChange: BikeStatusChange
    ): List<String> {
        val statusBikeMapper = StatusBikeMapper()
        return listOf(
            statusChange.bikeName,
            "From \"${statusBikeMapper.toString(lastStatusChange.toStatus)}\" ${formatTimestamp(lastStatusChange.timestamp)}",
            "To \"${statusBikeMapper.toString(statusChange.toStatus)}\" ${formatTimestamp(statusChange.timestamp)}",
            transitions[lastStatusChange.toStatus]?.get(statusChange.toStatus) ?: "???",
            "Duration ${formatDuration(statusChange.timestamp - lastStatusChange.timestamp)}",
            formatTimestamp(statusChange.timestamp)
        )
    }

    /**
     * Writes the status changes report to Google Sheets.
     *
     * @param statusChangesReport A list of lists representing the status changes report to be written to Google Sheets.
     */
    private fun writeToSheets(statusChangesReport: List<List<String>>) {
        val sheetsService = createSheetsService()
        val rows = sheetsService.spreadsheets().values()
            .get(Const.SHEET_ID, "${Const.SHEET2}!A:A")
            .execute()
            .getValues()
        val lastRowIndex = rows?.size ?: 0
        val range = "${Const.SHEET2}!A${lastRowIndex + 1}:E${lastRowIndex + statusChangesReport.size}"

        if (statusChangesReport.isNotEmpty()) {
            writeDataAndClear(sheetsService, Const.SHEET_ID, "${Const.SHEET3}!A1:E${statusChangesReport.size + 1000}", statusChangesReport)
            appendData(sheetsService, Const.SHEET_ID, range, statusChangesReport)
        }
    }

    /**
     * Creates a Sheets instance for interacting with the Google Sheets API.
     *
     * @return A Sheets instance for interacting with the Google Sheets API.
     */
    private fun createSheetsService(): Sheets {
        val httpTransport = NetHttpTransport()
        val jsonFactory: JsonFactory = JacksonFactory.getDefaultInstance()
        val serviceAccountPath =
            MyApplication.applicationContext().assets.open("augmented-ward-357019-f7f6d5778329.json")
        val credential =
            GoogleCredential.fromStream(serviceAccountPath, httpTransport, jsonFactory)
                .createScoped(listOf("https://www.googleapis.com/auth/spreadsheets"))
        return Sheets.Builder(httpTransport, jsonFactory, credential)
            .setApplicationName("My App Name")
            .build()
    }



    private fun startGlobalTimer() {
        scope.launch(Dispatchers.IO) {
            var down = Const.holderGoogle
            var prevActiveBikesSize = -1
            while (isActive) {
                val bikes = loadBikesData()
                val activeBikes = bikes.filter { it.status == StatusBike.ACTIVE || it.status == StatusBike.WAIT_FOR_CANCEL }
                val updatedBikes = bikes.map { bike ->
                    if (bike.endTime == 1L && bike.status == StatusBike.IDLE) {
                        bike.endTime = 0L
                    }
                    if (bike.status == StatusBike.CANCELED) {
                        bike.status = StatusBike.IDLE
                    }

                    if (bike.status == StatusBike.ACTIVE || bike.status == StatusBike.WAIT_FOR_CANCEL) {
                        bike.endTime = bike.startTime + bike.rentDuration * 60 * 1000 - System.currentTimeMillis()
                        if (bike.endTime <= Const.alarmTimerHolder) {
                            if (bike.status != StatusBike.WAIT_FOR_CANCEL) {
                                bike.status = StatusBike.WAIT_FOR_CANCEL
                                processBikeHistory(bike)
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

                if (activeBikes.size != prevActiveBikesSize || (--down < 0)) {
                    sliceCurrentActivity(activeBikes, activeBikes.size != prevActiveBikesSize)
                    prevActiveBikesSize = activeBikes.size
                    down = Const.holderGoogle
                }

                delay(500)
            }
        }
    }

    /**
     * Сохраняет данные по текущей активности в Google Sheets
     *
     * @param bikes список велосипедов, находящихся в текущей активности
     * @param delete флаг, указывающий на необходимость удаления данных из таблицы
     */
    private suspend fun sliceCurrentActivity(bikes: List<Bike>, delete: Boolean) {
        val values = bikes
            .filter { it.status == StatusBike.ACTIVE || it.status == StatusBike.WAIT_FOR_CANCEL }
            .map { bike ->
                val start = formatTimestamp(bike.startTime)
                val end = formatTimestamp(bike.startTime + bike.rentDuration * 60 * 1000)
                val remaining = formatDuration(bike.endTime)
                listOf(bike.name, start, end, remaining)
            }

        if (values.isNotEmpty() || delete) {
            val sheetsService = getSheetsService()

            if (delete) {
                writeDataAndClear(
                    sheetsService,
                    Const.SHEET_ID,
                    "${Const.SHEET1}!A1:D${values.size + 1000}",
                    values
                )
            } else {
                writeData(
                    sheetsService,
                    Const.SHEET_ID,
                    "${Const.SHEET1}!A1:D${values.size + 1}",
                    values
                )
            }
        }
    }


    /**
     * Получает экземпляр класса Sheets с использованием учетных данных из файла json.
     *
     * @return экземпляр класса Sheets
     */
    private fun getSheetsService(): Sheets {
        val httpTransport = NetHttpTransport()
        val jsonFactory: JsonFactory = JacksonFactory.getDefaultInstance()
        val credentials = GoogleCredential.fromStream(
            MyApplication.applicationContext().assets.open(Const.GOOGLE_CREDENTIAL_FILE)
        )
        val scopedCredentials = credentials.createScoped(listOf("https://www.googleapis.com/auth/spreadsheets"))

        return Sheets.Builder(httpTransport, jsonFactory, scopedCredentials)
            .setApplicationName("My App Name")
            .build()
    }

    private fun formatTimestamp(timestamp: Long): String {
        val date = Date(timestamp)
        val format = SimpleDateFormat("dd.MM HH:mm:ss", Locale.getDefault())
        return format.format(date)
    }

    private fun formatDuration(timestamp: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(timestamp)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timestamp)
        val seconds =
            TimeUnit.MILLISECONDS.toSeconds(timestamp - TimeUnit.MINUTES.toMillis(minutes))
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
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


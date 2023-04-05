package com.bicycle.model

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AlertDialog
import com.bicycle.MainActivity
import com.bicycle.MyApplication
import com.bicycle.util.Const
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.ClearValuesRequest
import com.google.api.services.sheets.v4.model.ValueRange
import kotlinx.coroutines.delay
import java.io.FileInputStream
import java.util.*


enum class StatusBike {
    IDLE, ACTIVE, WAIT_FOR_CANCEL, CANCELED
}

val transitions = mapOf(
    StatusBike.IDLE to mapOf(
        Pair(StatusBike.ACTIVE, "Арендован")
    ),
    StatusBike.ACTIVE to mapOf(
        Pair(StatusBike.WAIT_FOR_CANCEL, "Время аренды вышло"),
        Pair(StatusBike.CANCELED, "Заказ отменен"),
        Pair(StatusBike.IDLE, "Кассир закрыл заказ раньше времени")
    ),
    StatusBike.WAIT_FOR_CANCEL to mapOf(
        Pair(StatusBike.IDLE, "Кассир закрыл заказ")
    ),
    StatusBike.CANCELED to mapOf(
        Pair(StatusBike.ACTIVE, "Арендован")
    )
)

class StatusBikeMapper {

    fun toString(statusBike: StatusBike): String {
        return when (statusBike) {
            StatusBike.IDLE -> "бездействие"
            StatusBike.ACTIVE -> "арендован"
            StatusBike.WAIT_FOR_CANCEL -> "ожидание закрытия поездки кассиром"
            StatusBike.CANCELED -> "отменен"
        }
    }

    fun fromString(str: String): StatusBike {
        return when (str.toLowerCase(Locale.getDefault())) {
            "idle" -> StatusBike.IDLE
            "active" -> StatusBike.ACTIVE
            "wait_for_cancel" -> StatusBike.WAIT_FOR_CANCEL
            else -> throw IllegalArgumentException("Unknown statusBike string: $str")
        }
    }
}

data class Bike(
    val id: Int,
    var status: StatusBike,
    val name: String,
    val price: String,
    val rentDuration: Long,
    var startTime: Long,
    var endTime: Long,
    val color: Int
)

//val spreadsheetId = "1JW4gj9kL9yRwOIjE0V8uhNdQNrCH2ioPewwdZkFnf_E"
fun main() {
    val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
    val jsonFactory: JsonFactory = JacksonFactory.getDefaultInstance()
    //val serviceAccountPath = "path/to/your-service-account-key.json"
    val serviceAccountPath = "app\\src\\main\\assets\\augmented-ward-357019-f7f6d5778329.json"

    val credential =
        GoogleCredential.fromStream(FileInputStream(serviceAccountPath), httpTransport, jsonFactory)
            .createScoped(listOf("https://www.googleapis.com/auth/spreadsheets"))

    val sheetsService = Sheets.Builder(httpTransport, jsonFactory, credential)
        .setApplicationName("My App Name")
        .build()

    //val spreadsheetId = "1PK5BmTTxdPLjA8zIdTNvujxzhMPiR5hYmVjuoKPbrvM"
    val spreadsheetId = Const.sheetId
    val range = "${Const.sheet1}!A1:D2"
    val values = listOf(
        listOf("A1", "B1", "C1", "D1"),
        listOf("A2", "B2", "C2", "D2")
    )
    //writeData(sheetsService, spreadsheetId, range, values)
}

fun readData(sheetsService: Sheets, spreadsheetId: String, range: String): List<List<Any>> {
    val response = sheetsService.spreadsheets().values()[spreadsheetId, range].execute()
    return response.getValues() ?: emptyList()
}

suspend fun writeData(
    sheetsService: Sheets,
    spreadsheetId: String,
    range: String,
    values: List<List<String?>>
) {
    try {
        val body = ValueRange().setValues(values)
        sheetsService.spreadsheets().values()
            .update(spreadsheetId, range, body)
            .setValueInputOption("RAW")
            .execute()
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
        delay(30000)
    }
}

fun appendData(
    sheetsService: Sheets,
    spreadsheetId: String,
    range: String,
    values: List<List<String?>>
) {


    try {
        val body = ValueRange().setValues(values)
        val appendRequest = sheetsService.spreadsheets().values()
            .append(spreadsheetId, range, body)
        appendRequest.valueInputOption = "RAW"
        appendRequest.execute()
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

fun writeDataAndClear(
    sheetsService: Sheets,
    spreadsheetId: String,
    range: String,
    values: List<List<Any>>
) {
    try {
        val body = ValueRange().setValues(values)
        sheetsService.spreadsheets().values()
            .clear(spreadsheetId, range, ClearValuesRequest())
            .execute()
        sheetsService.spreadsheets().values()
            .update(spreadsheetId, range, body)
            .setValueInputOption("RAW")
            .execute()
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
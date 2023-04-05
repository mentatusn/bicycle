package com.bicycle.model

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.ClearValuesRequest
import com.google.api.services.sheets.v4.model.ValueRange
import java.util.*


/**
An enum representing the various statuses of a bike rental.
 */
enum class StatusBike {
    IDLE, ACTIVE, WAIT_FOR_CANCEL, CANCELED
}

/**
A map that defines the allowed transitions between bike rental statuses, along with a description
of each transition.
 */
val transitions = mapOf(
    StatusBike.IDLE to mapOf(
        Pair(StatusBike.ACTIVE, "Rented")
    ),
    StatusBike.ACTIVE to mapOf(
        Pair(StatusBike.WAIT_FOR_CANCEL, "Rental time expired"),
        Pair(StatusBike.CANCELED, "Order canceled"),
        Pair(StatusBike.IDLE, "Cashier closed the order ahead of time")
    ),
    StatusBike.WAIT_FOR_CANCEL to mapOf(
        Pair(StatusBike.IDLE, "Cashier closed the order")
    ),
    StatusBike.CANCELED to mapOf(
        Pair(StatusBike.ACTIVE, "Rented")
    )
)


class StatusBikeMapper {


    fun toString(statusBike: StatusBike): String {
        return when (statusBike) {
            StatusBike.IDLE -> "Idle"
            StatusBike.ACTIVE -> "Rented"
            StatusBike.WAIT_FOR_CANCEL -> "Waiting for cashier to close order"
            StatusBike.CANCELED -> "Canceled"
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

/**
 * A data class representing a bike for rental.
 * @param id the unique ID of the bike
 * @param status the current status of the bike rental
 * @param name the name of the bike
 * @param price the rental price of the bike
 * @param rentDuration the duration of the bike rental in minutes
 * @param startTime the start time of the bike rental
 * @param endTime the end time of the bike rental
 * @param color the color of the bike (as an Android resource ID)
 */
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

/**
 * Writes data to a Google Sheet.
 * @param context the context of the caller
 * @param sheetsService the Google Sheets service object
 * @param spreadsheetId the ID of the Google Sheet
 * @param range the cell range to write to
 * @param values the data to write to the sheet
 */
@Throws(GoogleJsonResponseException::class)
suspend fun writeData(
    sheetsService: Sheets,
    spreadsheetId: String,
    range: String,
    values: List<List<String?>>
) {

    val body = ValueRange().setValues(values)
    sheetsService.spreadsheets().values()
        .update(spreadsheetId, range, body)
        .setValueInputOption("RAW")
        .execute()
    //delay(30000)
}

/**
 * Appends a list of rows to the specified range in the given Google Sheets spreadsheet.
 *
 * @param context the context of the calling activity
 * @param sheetsService the Sheets service to use for the operation
 * @param spreadsheetId the ID of the spreadsheet to write to
 * @param range the range of the sheet to write to, e.g. "Sheet1!A1:C3"
 * @param values the list of rows to write to the sheet
 */
@Throws(GoogleJsonResponseException::class)
fun appendData(
    sheetsService: Sheets,
    spreadsheetId: String,
    range: String,
    values: List<List<String?>>
) {
    val body = ValueRange().setValues(values)
    val appendRequest = sheetsService.spreadsheets().values()
        .append(spreadsheetId, range, body)
    appendRequest.valueInputOption = "RAW"
    appendRequest.execute()
}

/**
 * Writes data to a Google Sheets spreadsheet and clears any existing data in the specified range.
 * If an error occurs, an alert dialog is displayed.
 * @param context the context of the calling activity
 * @param sheetsService The Sheets API service object.
 * @param spreadsheetId The ID of the spreadsheet.
 * @param range The range of cells to write data to.
 * @param values The data to write to the spreadsheet.
 */
@Throws(GoogleJsonResponseException::class)
fun writeDataAndClear(
    sheetsService: Sheets,
    spreadsheetId: String,
    range: String,
    values: List<List<Any>>
) {
    val body = ValueRange().setValues(values)
    sheetsService.spreadsheets().values()
        .clear(spreadsheetId, range, ClearValuesRequest())
        .execute()
    sheetsService.spreadsheets().values()
        .update(spreadsheetId, range, body)
        .setValueInputOption("RAW")
        .execute()
}



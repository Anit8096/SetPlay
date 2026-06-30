package com.kmp.setplay.presentation.common

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Read-only text field that opens a Material3 [DatePickerDialog] on tap.
 * Pure Compose Multiplatform — works identically on Android, JS, and WasmJS,
 * so no expect/actual split is needed.
 *
 * @param minDate if set, dates before this are not selectable in the dialog
 *   (greyed out / disabled, not just rejected after the fact).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    label: String,
    date: LocalDate?,
    onDateChanged: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "yyyy-mm-dd",
    minDate: LocalDate? = null
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }

    OutlinedTextField(
        value = date?.toString() ?: "",
        onValueChange = {},
        placeholder = { Text(placeholder) },
        readOnly = true,
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        trailingIcon = {
            IconButton(onClick = { showDialog = true }) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = "Pick $label")
            }
        },
        modifier = modifier
    )

    if (showDialog) {
        val minMillis = minDate?.atStartOfDayMillisUtc()
        val selectableDates = remember(minMillis) {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    minMillis == null || utcTimeMillis >= minMillis

                override fun isSelectableYear(year: Int): Boolean =
                    minMillis == null || year >= minDate.year
            }
        }

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = date?.atStartOfDayMillisUtc(),
            selectableDates = selectableDates
        )

        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    onDateChanged(datePickerState.selectedDateMillis?.toLocalDateUtc())
                    showDialog = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

/** Today's date in the device's local time zone — used as the default minimum for start dates. */
fun todayLocalDate(): LocalDate =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

// ── Conversion helpers (UTC, date-only — no time-of-day concerns) ──────────────

private fun LocalDate.atStartOfDayMillisUtc(): Long =
    LocalDateTime(this, LocalTime(0, 0)).toInstant(TimeZone.UTC).toEpochMilliseconds()

private fun Long.toLocalDateUtc(): LocalDate =
    Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.UTC).date

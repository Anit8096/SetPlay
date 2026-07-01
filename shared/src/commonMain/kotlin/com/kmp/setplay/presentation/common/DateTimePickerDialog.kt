package com.kmp.setplay.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * A single dialog combining a Material3 [DatePicker] and [TimePicker], used to set the
 * date/time shown under a bracket match. Confirms both at once into a single [Instant].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchDateTimePickerDialog(
    initial: Instant?,
    onConfirm: (Instant) -> Unit,
    onClear: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val tz = TimeZone.currentSystemDefault()
    val initialLdt = initial?.toLocalDateTime(tz)

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialLdt?.date
            ?.let { LocalDateTime(it, LocalTime(0, 0)).toInstant(TimeZone.UTC).toEpochMilliseconds() }
    )
    val timePickerState = rememberTimePickerState(
        initialHour = initialLdt?.hour ?: 12,
        initialMinute = initialLdt?.minute ?: 0,
        is24Hour = false
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set date & time") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DatePicker(state = datePickerState, showModeToggle = false)
                HorizontalDivider()
                TimePicker(state = timePickerState)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val dateMillis = datePickerState.selectedDateMillis
                if (dateMillis != null) {
                    val date = Instant.fromEpochMilliseconds(dateMillis)
                        .toLocalDateTime(TimeZone.UTC).date
                    val ldt = LocalDateTime(date, LocalTime(timePickerState.hour, timePickerState.minute))
                    onConfirm(ldt.toInstant(tz))
                }
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = {
            Column {
                if (onClear != null) {
                    TextButton(onClick = { onClear(); onDismiss() }) { Text("Clear") }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

/** Formats a match's scheduled Instant for display, e.g. "Jul 5, 3:30 PM". */
fun formatMatchSchedule(instant: Instant): String {
    val ldt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val month = months[ldt.month.number - 1]
    val hour12 = when {
        ldt.hour == 0  -> 12
        ldt.hour > 12  -> ldt.hour - 12
        else           -> ldt.hour
    }
    val ampm = if (ldt.hour < 12) "AM" else "PM"
    val minute = ldt.minute.toString().padStart(2, '0')
    return "$month ${ldt.day}, $hour12:$minute $ampm"
}

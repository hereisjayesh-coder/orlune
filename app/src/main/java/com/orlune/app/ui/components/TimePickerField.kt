package com.orlune.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/**
 * A tappable field showing an "HH:mm" value that opens a Material time-picker dialog
 * — replaces manually typed HH:mm text entry. [time] and the value passed to
 * [onTimeChange] are both "HH:mm" strings, matching ScheduleEntity's stored format
 * (see ScheduleEngine.parse) — this widget only changes how the value is entered,
 * not what's stored.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerField(label: String, time: String, onTimeChange: (String) -> Unit, modifier: Modifier = Modifier) {
    var showDialog by remember { mutableStateOf(false) }
    val parsed = remember(time) { runCatching { LocalTime.parse(time) }.getOrDefault(LocalTime.MIDNIGHT) }

    Column(modifier = modifier.clickable { showDialog = true }.padding(vertical = 8.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(parsed.format(TIME_FORMAT), style = MaterialTheme.typography.headlineSmall)
    }

    if (showDialog) {
        val state = rememberTimePickerState(initialHour = parsed.hour, initialMinute = parsed.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    onTimeChange(LocalTime.of(state.hour, state.minute).format(TIME_FORMAT))
                    showDialog = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } },
            title = { Text(label) },
            text = { TimePicker(state = state, modifier = Modifier.padding(top = 8.dp)) }
        )
    }
}

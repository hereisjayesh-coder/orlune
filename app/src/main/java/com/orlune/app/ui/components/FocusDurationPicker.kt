package com.orlune.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val FOCUS_DURATION_PRESETS_MINUTES = listOf(15, 25, 30, 45, 60, 90)

/**
 * A familiar, Android-alarm-style duration picker: preset chips for the common cases,
 * plus "Custom" opening a Material3 [TimePicker] dial for anything else. The dial is
 * deliberately repurposed rather than replaced with a hand-built clock — per the
 * product decision to reuse the platform's own time-selection affordance instead of
 * inventing a new one. [TimePickerState.hour]/[minute] are read purely as an
 * hours+minutes *duration* here, never as a wall-clock time: nothing in this
 * component, or in what it writes back through [onMinutesChange], ever becomes a
 * start-time or a `LocalTime` — [minutesText] is, and stays, a plain elapsed-minutes
 * count, same as every other duration field in this codebase
 * (`OnboardingDailyLimit`, `DailyLimitInput`, `RuleRepository.addDailyLimit`).
 * `is24Hour = true` is what removes the AM/PM affordance, since a duration has no
 * "which half of the day" concept to begin with.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusDurationPicker(minutesText: String, onMinutesChange: (String) -> Unit) {
    val currentMinutes = minutesText.toIntOrNull()
    var showCustomDialog by remember { mutableStateOf(false) }

    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            FOCUS_DURATION_PRESETS_MINUTES.forEach { preset ->
                FilterChip(
                    selected = currentMinutes == preset,
                    onClick = { onMinutesChange(preset.toString()) },
                    label = { Text("${preset}m") }
                )
            }
            FilterChip(
                selected = currentMinutes != null && currentMinutes !in FOCUS_DURATION_PRESETS_MINUTES,
                onClick = { showCustomDialog = true },
                label = { Text("Custom") }
            )
        }
        Text(
            if (currentMinutes != null && currentMinutes > 0) "= ${formatDuration(currentMinutes * 60L)}" else "Choose a duration",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(top = 8.dp)
                .clickable { showCustomDialog = true }
        )
    }

    if (showCustomDialog) {
        val initialTotal = (currentMinutes ?: 25).coerceIn(1, 23 * 60 + 59)
        val state = rememberTimePickerState(initialHour = initialTotal / 60, initialMinute = initialTotal % 60, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = { Text("Focus duration") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TimePicker(state = state)
                    Text(
                        "This sets how long Focus runs, not a start time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val totalMinutes = (state.hour * 60 + state.minute).coerceAtLeast(1)
                    onMinutesChange(totalMinutes.toString())
                    showCustomDialog = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showCustomDialog = false }) { Text("Cancel") } }
        )
    }
}

package com.orlune.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val FOCUS_DURATION_PRESETS_MINUTES = listOf(25, 45, 60, 90)

/** 25m/45m/60m/90m/Custom preset chips for Focus duration — same
 * preset-plus-"Custom"-falls-through-to-free-entry pattern as [WeekdaySelector],
 * layered above the existing minutes text field rather than replacing it, since an
 * arbitrary custom duration still needs free entry. [minutesText] is the same state
 * the text field below already owns; tapping a preset just sets it, tapping "Custom"
 * is a no-op (it's already reachable by typing) and only exists so the row always has
 * a chip reflecting the current selection. */
@Composable
fun DurationPresetSelector(minutesText: String, onMinutesChange: (String) -> Unit) {
    val currentMinutes = minutesText.toIntOrNull()
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
        FOCUS_DURATION_PRESETS_MINUTES.forEach { preset ->
            FilterChip(
                selected = currentMinutes == preset,
                onClick = { onMinutesChange(preset.toString()) },
                label = { Text("${preset}m") }
            )
        }
        FilterChip(
            selected = currentMinutes == null || currentMinutes !in FOCUS_DURATION_PRESETS_MINUTES,
            onClick = {},
            label = { Text("Custom") }
        )
    }
}

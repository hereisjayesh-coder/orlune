package com.orlune.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Canonical week order, matching ScheduleEngine's day codes — the source of truth
 * for how a selected-days Set<String> is serialized back to ScheduleEntity.daysOfWeek's
 * comma-separated string. */
val WEEKDAY_ORDER = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")

private val WEEKDAY_LABELS = mapOf(
    "MON" to "Mon", "TUE" to "Tue", "WED" to "Wed", "THU" to "Thu",
    "FRI" to "Fri", "SAT" to "Sat", "SUN" to "Sun"
)
private val WEEKDAYS = setOf("MON", "TUE", "WED", "THU", "FRI")
private val WEEKEND_DAYS = setOf("SAT", "SUN")
private val EVERY_DAY = WEEKDAY_ORDER.toSet()

private data class SchedulePreset(val label: String, val days: Set<String>?)

private val PRESETS = listOf(
    SchedulePreset("Every day", EVERY_DAY),
    SchedulePreset("Weekdays", WEEKDAYS),
    SchedulePreset("Weekends", WEEKEND_DAYS),
    SchedulePreset("Custom", null)
)

/** Individual Mon–Sun toggles plus Every day/Weekdays/Weekends/Custom presets —
 * replaces manually typed day-code text entry (e.g. "MON,WED"). [selectedDays] and
 * [onDaysChange] both use ScheduleEntity's day codes (MON, TUE, ...), never a raw
 * comma string, so duplicate or malformed entries are structurally impossible. */
@Composable
fun WeekdaySelector(selectedDays: Set<String>, onDaysChange: (Set<String>) -> Unit) {
    Column {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            PRESETS.forEach { preset ->
                val isSelected = if (preset.days == null) {
                    PRESETS.none { it.days != null && it.days == selectedDays }
                } else {
                    preset.days == selectedDays
                }
                FilterChip(
                    selected = isSelected,
                    onClick = { preset.days?.let(onDaysChange) },
                    label = { Text(preset.label) }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            WEEKDAY_ORDER.forEach { day ->
                FilterChip(
                    selected = day in selectedDays,
                    onClick = {
                        onDaysChange(if (day in selectedDays) selectedDays - day else selectedDays + day)
                    },
                    label = { Text(WEEKDAY_LABELS.getValue(day)) }
                )
            }
        }
    }
}

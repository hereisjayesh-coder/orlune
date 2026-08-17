package com.orlune.app.ui.components

import com.orlune.app.core.domain.rules.ScheduleEngine
import com.orlune.app.data.local.entity.ScheduleEntity

fun formatDuration(totalSeconds: Long): String {
    val safeSeconds = totalSeconds.coerceAtLeast(0)
    val totalMinutes = safeSeconds / 60
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

fun signedDuration(seconds: Long): String = if (seconds < 0) "−${formatDuration(-seconds)}" else "+${formatDuration(seconds)}"

fun isValidSchedule(days: String, start: String, end: String): Boolean {
    if (days.isBlank()) return false
    return runCatching {
        ScheduleEngine.parse(
            ScheduleEntity(
                name = "validation",
                daysOfWeek = days.split(",").map { it.trim().uppercase() }.filter { it.isNotEmpty() }.joinToString(","),
                startTime = start,
                endTime = end,
                associatedRuleId = 0L
            )
        )
    }.isSuccess
}

package com.orlune.app.core.domain.rules

import com.orlune.app.data.local.entity.ScheduleEntity
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

data class ScheduleWindow(
    val daysOfWeek: Set<DayOfWeek>,
    val startTime: LocalTime,
    val endTime: LocalTime
)

private val DAY_CODES = mapOf(
    "MON" to DayOfWeek.MONDAY,
    "TUE" to DayOfWeek.TUESDAY,
    "WED" to DayOfWeek.WEDNESDAY,
    "THU" to DayOfWeek.THURSDAY,
    "FRI" to DayOfWeek.FRIDAY,
    "SAT" to DayOfWeek.SATURDAY,
    "SUN" to DayOfWeek.SUNDAY
)

/**
 * Evaluates a recurring time window, per `docs/phase-0-research.md` Section 9
 * (ScheduleEngine: "currentTime ∈ [schedule.start, schedule.end] on
 * currentDayOfWeek"). The window is half-open — [startTime, endTime) — so back-to-back
 * schedules never overlap at their shared boundary.
 */
object ScheduleEngine {

    fun parse(entity: ScheduleEntity): ScheduleWindow = ScheduleWindow(
        daysOfWeek = entity.daysOfWeek.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { code -> DAY_CODES.getValue(code) }
            .toSet(),
        startTime = LocalTime.parse(entity.startTime),
        endTime = LocalTime.parse(entity.endTime)
    )

    /**
     * [ScheduleWindow.startTime] > [ScheduleWindow.endTime] means the window crosses
     * midnight (e.g. 22:00–06:00). An overnight window is active in two segments: the
     * evening half on a scheduled day itself, and the morning half on the day *after*
     * a scheduled day — so a "MON 22:00–06:00" schedule is still active at 05:00 on
     * Tuesday, even though Tuesday isn't in [ScheduleWindow.daysOfWeek].
     */
    fun isActive(window: ScheduleWindow, at: LocalDateTime): Boolean {
        val time = at.toLocalTime()
        val day = at.dayOfWeek
        return if (window.startTime <= window.endTime) {
            day in window.daysOfWeek && time >= window.startTime && time < window.endTime
        } else {
            (day in window.daysOfWeek && time >= window.startTime) ||
                (day.minus(1) in window.daysOfWeek && time < window.endTime)
        }
    }
}

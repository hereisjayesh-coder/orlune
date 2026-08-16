package com.orlune.app.core.domain.rules

import com.orlune.app.data.local.entity.ScheduleEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

/** 2024-01-01 is a known Monday; the rest of the reference week follows from it. */
class ScheduleEngineTest {

    @Test
    fun `parse splits comma-separated days and HH mm times`() {
        val window = ScheduleEngine.parse(
            ScheduleEntity(daysOfWeek = "MON,WED,FRI", startTime = "09:00", endTime = "17:30", associatedRuleId = 1)
        )

        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), window.daysOfWeek)
        assertEquals(LocalTime.of(9, 0), window.startTime)
        assertEquals(LocalTime.of(17, 30), window.endTime)
    }

    @Test
    fun `parse handles a single day`() {
        val window = ScheduleEngine.parse(
            ScheduleEntity(daysOfWeek = "SUN", startTime = "00:00", endTime = "23:59", associatedRuleId = 1)
        )

        assertEquals(setOf(DayOfWeek.SUNDAY), window.daysOfWeek)
    }

    @Test
    fun `same-day window is active inside the range on a scheduled day`() {
        val window = ScheduleWindow(setOf(DayOfWeek.MONDAY), LocalTime.of(9, 0), LocalTime.of(17, 0))

        assertTrue(ScheduleEngine.isActive(window, LocalDateTime.of(2024, 1, 1, 12, 0)))
    }

    @Test
    fun `same-day window is inactive before the range starts`() {
        val window = ScheduleWindow(setOf(DayOfWeek.MONDAY), LocalTime.of(9, 0), LocalTime.of(17, 0))

        assertFalse(ScheduleEngine.isActive(window, LocalDateTime.of(2024, 1, 1, 8, 59)))
    }

    @Test
    fun `same-day window is active exactly at the start time`() {
        val window = ScheduleWindow(setOf(DayOfWeek.MONDAY), LocalTime.of(9, 0), LocalTime.of(17, 0))

        assertTrue(ScheduleEngine.isActive(window, LocalDateTime.of(2024, 1, 1, 9, 0)))
    }

    @Test
    fun `same-day window is inactive exactly at the end time`() {
        val window = ScheduleWindow(setOf(DayOfWeek.MONDAY), LocalTime.of(9, 0), LocalTime.of(17, 0))

        assertFalse(ScheduleEngine.isActive(window, LocalDateTime.of(2024, 1, 1, 17, 0)))
    }

    @Test
    fun `same-day window is inactive on an unscheduled day even within the time range`() {
        val window = ScheduleWindow(setOf(DayOfWeek.MONDAY), LocalTime.of(9, 0), LocalTime.of(17, 0))

        assertFalse(ScheduleEngine.isActive(window, LocalDateTime.of(2024, 1, 2, 12, 0)))
    }

    @Test
    fun `overnight window is active in the evening segment on a scheduled day`() {
        val window = ScheduleWindow(setOf(DayOfWeek.MONDAY), LocalTime.of(22, 0), LocalTime.of(6, 0))

        assertTrue(ScheduleEngine.isActive(window, LocalDateTime.of(2024, 1, 1, 23, 0)))
    }

    @Test
    fun `overnight window is active exactly at the start time`() {
        val window = ScheduleWindow(setOf(DayOfWeek.MONDAY), LocalTime.of(22, 0), LocalTime.of(6, 0))

        assertTrue(ScheduleEngine.isActive(window, LocalDateTime.of(2024, 1, 1, 22, 0)))
    }

    @Test
    fun `overnight window is active in the morning segment on the day after a scheduled day`() {
        val window = ScheduleWindow(setOf(DayOfWeek.MONDAY), LocalTime.of(22, 0), LocalTime.of(6, 0))

        assertTrue(ScheduleEngine.isActive(window, LocalDateTime.of(2024, 1, 2, 5, 0)))
    }

    @Test
    fun `overnight window is inactive exactly at the end time the morning after`() {
        val window = ScheduleWindow(setOf(DayOfWeek.MONDAY), LocalTime.of(22, 0), LocalTime.of(6, 0))

        assertFalse(ScheduleEngine.isActive(window, LocalDateTime.of(2024, 1, 2, 6, 0)))
    }

    @Test
    fun `overnight window is inactive outside both segments`() {
        val window = ScheduleWindow(setOf(DayOfWeek.MONDAY), LocalTime.of(22, 0), LocalTime.of(6, 0))

        assertFalse(ScheduleEngine.isActive(window, LocalDateTime.of(2024, 1, 2, 12, 0)))
    }

    @Test
    fun `overnight window is inactive the morning after a non-adjacent unscheduled day`() {
        val window = ScheduleWindow(setOf(DayOfWeek.MONDAY), LocalTime.of(22, 0), LocalTime.of(6, 0))

        // Wednesday morning: Tuesday isn't scheduled, so the wraparound morning segment doesn't apply.
        assertFalse(ScheduleEngine.isActive(window, LocalDateTime.of(2024, 1, 3, 5, 0)))
    }

    @Test
    fun `overnight window wrapping into Monday is active early Monday morning`() {
        val window = ScheduleWindow(setOf(DayOfWeek.SUNDAY), LocalTime.of(22, 0), LocalTime.of(6, 0))

        // Sunday 2023-12-31 22:00-06:00 should still be active Monday 2024-01-01 at 05:00.
        assertTrue(ScheduleEngine.isActive(window, LocalDateTime.of(2024, 1, 1, 5, 0)))
    }
}

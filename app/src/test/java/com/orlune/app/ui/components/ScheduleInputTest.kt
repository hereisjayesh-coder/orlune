package com.orlune.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the day-selector and time-picker's underlying validation/serialization —
 * isValidSchedule (existing, shared with ScheduleEngine) and WEEKDAY_ORDER (new,
 * backs WeekdaySelector's Set<String> <-> ScheduleEntity.daysOfWeek round trip).
 */
class ScheduleInputTest {

    @Test
    fun `a normal same-day window with weekdays selected is valid`() {
        assertTrue(isValidSchedule("MON,TUE,WED,THU,FRI", "09:00", "17:00"))
    }

    @Test
    fun `an overnight window where start is after end is still valid`() {
        // ScheduleEngine.isActive interprets start > end as crossing midnight —
        // the picker must not reject this, since it's a legitimate use case
        // (e.g. a nightly 22:00-07:00 restriction).
        assertTrue(isValidSchedule("MON,TUE,WED,THU,FRI", "22:00", "07:00"))
    }

    @Test
    fun `an empty day selection is invalid`() {
        assertFalse(isValidSchedule("", "09:00", "17:00"))
    }

    @Test
    fun `a blank days string from an all-deselected WeekdaySelector is invalid`() {
        val selectedDays = emptySet<String>()
        val daysString = WEEKDAY_ORDER.filter { it in selectedDays }.joinToString(",")
        assertFalse(isValidSchedule(daysString, "09:00", "17:00"))
    }

    @Test
    fun `malformed time input is invalid`() {
        assertFalse(isValidSchedule("MON", "not-a-time", "17:00"))
    }

    @Test
    fun `a single selected day is valid`() {
        assertTrue(isValidSchedule("SAT", "10:00", "12:00"))
    }

    @Test
    fun `every day preset produces all seven canonical day codes in week order`() {
        val everyDay = WEEKDAY_ORDER.toSet()
        val daysString = WEEKDAY_ORDER.filter { it in everyDay }.joinToString(",")
        assertEquals("MON,TUE,WED,THU,FRI,SAT,SUN", daysString)
    }

    @Test
    fun `serializing a WeekdaySelector selection always follows week order, not selection order`() {
        // Simulates a user tapping Friday before Monday — the stored string must
        // still read Monday-first, matching ScheduleEngine's DAY_CODES ordering
        // expectations and keeping "Active rules" display deterministic.
        val selectedOutOfOrder = setOf("FRI", "MON")
        val daysString = WEEKDAY_ORDER.filter { it in selectedOutOfOrder }.joinToString(",")
        assertEquals("MON,FRI", daysString)
    }

    @Test
    fun `deselecting a day removes only that day from the serialized string`() {
        var selected = WEEKDAY_ORDER.toSet()
        selected = selected - "WED"
        val daysString = WEEKDAY_ORDER.filter { it in selected }.joinToString(",")
        assertEquals("MON,TUE,THU,FRI,SAT,SUN", daysString)
    }
}

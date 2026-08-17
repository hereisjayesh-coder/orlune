package com.orlune.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class DurationStepperTest {

    @Test
    fun `incrementing within range increases by the step`() {
        assertEquals(6, steppedValue(current = 5, delta = 1, range = 0..24))
    }

    @Test
    fun `incrementing at the top of the range stays clamped, never overflowing`() {
        assertEquals(24, steppedValue(current = 24, delta = 1, range = 0..24))
    }

    @Test
    fun `decrementing at the bottom of the range stays clamped, never going negative`() {
        assertEquals(0, steppedValue(current = 0, delta = -1, range = 0..24))
    }

    @Test
    fun `minutes step by 5 and clamp at the 55 ceiling used for custom durations`() {
        assertEquals(55, steppedValue(current = 50, delta = 5, range = 0..55))
        assertEquals(55, steppedValue(current = 55, delta = 5, range = 0..55))
    }

    @Test
    fun `a mid-range value decrements normally`() {
        assertEquals(45, steppedValue(current = 50, delta = -5, range = 0..55))
    }

    @Test
    fun `a 2h 0m stepper selection previews as 2h 0m`() {
        val hours = 2; val minutes = 0
        val seconds = (hours * 3600L) + (minutes * 60L)
        assertEquals("2h 0m", formatDuration(seconds))
    }

    @Test
    fun `a 3h 30m stepper selection previews as 3h 30m`() {
        val hours = 3; val minutes = 30
        val seconds = (hours * 3600L) + (minutes * 60L)
        assertEquals("3h 30m", formatDuration(seconds))
    }

    @Test
    fun `a 4h 0m stepper selection previews as 4h 0m`() {
        val hours = 4; val minutes = 0
        val seconds = (hours * 3600L) + (minutes * 60L)
        assertEquals("4h 0m", formatDuration(seconds))
    }
}

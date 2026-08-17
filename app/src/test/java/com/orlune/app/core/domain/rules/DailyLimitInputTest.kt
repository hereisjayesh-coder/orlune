package com.orlune.app.core.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyLimitInputTest {

    @Test
    fun `preset 30 minutes converts to 1800 seconds`() {
        assertEquals(1800L, DailyLimitInput.toThresholdSeconds(hours = 0, minutes = 30).getOrThrow())
    }

    @Test
    fun `preset 90 minutes, decomposed to 1 hour 30 minutes, converts to 5400 seconds`() {
        // Presets are flat minute counts (e.g. 90) in the UI, decomposed into
        // hours+minutes before reaching this function — see LimitsScreen.
        assertEquals(5400L, DailyLimitInput.toThresholdSeconds(hours = 1, minutes = 30).getOrThrow())
    }

    @Test
    fun `a flat 90 passed as minutes without decomposing is rejected`() {
        assertTrue(DailyLimitInput.toThresholdSeconds(hours = 0, minutes = 90).isFailure)
    }

    @Test
    fun `custom 2 hours converts to 7200 seconds`() {
        assertEquals(7200L, DailyLimitInput.toThresholdSeconds(hours = 2, minutes = 0).getOrThrow())
    }

    @Test
    fun `custom 5 hours 30 minutes converts to 19800 seconds`() {
        assertEquals(19800L, DailyLimitInput.toThresholdSeconds(hours = 5, minutes = 30).getOrThrow())
    }

    @Test
    fun `custom 4 hours converts to 14400 seconds`() {
        assertEquals(14400L, DailyLimitInput.toThresholdSeconds(hours = 4, minutes = 0).getOrThrow())
    }

    @Test
    fun `negative hours is rejected`() {
        assertTrue(DailyLimitInput.toThresholdSeconds(hours = -1, minutes = 0).isFailure)
    }

    @Test
    fun `negative minutes is rejected`() {
        assertTrue(DailyLimitInput.toThresholdSeconds(hours = 0, minutes = -1).isFailure)
    }

    @Test
    fun `minutes of 60 or more is rejected`() {
        assertTrue(DailyLimitInput.toThresholdSeconds(hours = 1, minutes = 60).isFailure)
    }

    @Test
    fun `zero total duration is rejected`() {
        assertTrue(DailyLimitInput.toThresholdSeconds(hours = 0, minutes = 0).isFailure)
    }

    @Test
    fun `one minute total is the minimum allowed`() {
        assertEquals(60L, DailyLimitInput.toThresholdSeconds(hours = 0, minutes = 1).getOrThrow())
    }

    @Test
    fun `exactly 24 hours is allowed`() {
        assertEquals(86400L, DailyLimitInput.toThresholdSeconds(hours = 24, minutes = 0).getOrThrow())
    }

    @Test
    fun `more than 24 hours is rejected`() {
        assertTrue(DailyLimitInput.toThresholdSeconds(hours = 24, minutes = 1).isFailure)
    }
}

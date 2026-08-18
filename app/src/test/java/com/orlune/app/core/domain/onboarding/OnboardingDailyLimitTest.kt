package com.orlune.app.core.domain.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingDailyLimitTest {

    @Test
    fun `a valid custom duration with a selected app produces a plan`() {
        val plan = OnboardingDailyLimit.plan(
            skipped = false,
            hours = 0,
            minutes = 20,
            selectedPackages = listOf("com.example.social")
        )
        assertEquals(OnboardingDailyLimit.Plan(listOf("com.example.social"), 1200L), plan)
    }

    @Test
    fun `a preset duration (60 minutes) with two selected apps produces a plan covering both`() {
        val plan = OnboardingDailyLimit.plan(
            skipped = false,
            hours = 1,
            minutes = 0,
            selectedPackages = listOf("com.example.social", "com.example.video")
        )
        assertEquals(listOf("com.example.social", "com.example.video"), plan?.packages)
        assertEquals(3600L, plan?.thresholdSeconds)
    }

    @Test
    fun `zero-minute custom duration produces no plan even with apps selected`() {
        // This is the exact bug this file exists to prevent regressing: a 0-minute
        // custom duration must never silently produce a rule (or silently produce
        // nothing without the UI knowing why) — the UI is expected to keep Continue
        // disabled via isValidDuration, but this is the last line of defense.
        val plan = OnboardingDailyLimit.plan(
            skipped = false,
            hours = 0,
            minutes = 0,
            selectedPackages = listOf("com.example.social")
        )
        assertNull(plan)
    }

    @Test
    fun `skipping the daily-limit step produces no plan regardless of duration`() {
        val plan = OnboardingDailyLimit.plan(
            skipped = true,
            hours = 1,
            minutes = 0,
            selectedPackages = listOf("com.example.social")
        )
        assertNull(plan)
    }

    @Test
    fun `a valid duration with no apps selected produces no plan`() {
        val plan = OnboardingDailyLimit.plan(
            skipped = false,
            hours = 1,
            minutes = 0,
            selectedPackages = emptyList()
        )
        assertNull(plan)
    }

    @Test
    fun `isValidDuration is false for zero minutes and true for any preset`() {
        assertTrue(OnboardingDailyLimit.isValidDuration(0, 0).not())
        assertTrue(OnboardingDailyLimit.isValidDuration(0, 30))
        assertTrue(OnboardingDailyLimit.isValidDuration(1, 30))
    }

    @Test
    fun `isValidDuration is false beyond the 24-hour ceiling`() {
        assertTrue(OnboardingDailyLimit.isValidDuration(24, 1).not())
        assertTrue(OnboardingDailyLimit.isValidDuration(24, 0))
    }
}

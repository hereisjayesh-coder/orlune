package com.orlune.app.core.domain.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingGoalTest {

    @Test
    fun `fromStored round-trips every real enum value`() {
        OnboardingGoal.entries.forEach {
            assertEquals(it, OnboardingGoal.fromStored(it.name))
        }
    }

    @Test
    fun `fromStored returns null for an unrecognized value rather than crashing`() {
        assertNull(OnboardingGoal.fromStored("not-a-real-goal"))
        assertNull(OnboardingGoal.fromStored(""))
    }

    @Test
    fun `parseOnboardingGoals parses, trims, and dedupes comma-separated names`() {
        val goals = parseOnboardingGoals(" FOCUS ,STUDY,FOCUS,,REST")
        assertEquals(setOf(OnboardingGoal.FOCUS, OnboardingGoal.STUDY, OnboardingGoal.REST), goals)
    }

    @Test
    fun `parseOnboardingGoals is empty for a blank stored value`() {
        assertTrue(parseOnboardingGoals("").isEmpty())
        assertTrue(parseOnboardingGoals("   ").isEmpty())
    }

    @Test
    fun `parseOnboardingGoals drops unrecognized tokens instead of failing the whole set`() {
        val goals = parseOnboardingGoals("FOCUS,not-a-real-goal,REST")
        assertEquals(setOf(OnboardingGoal.FOCUS, OnboardingGoal.REST), goals)
    }

    @Test
    fun `every goal round-trips through join and parse`() {
        val all = OnboardingGoal.entries.toSet()
        val joined = all.joinToString(",") { it.name }
        assertEquals(all, parseOnboardingGoals(joined))
    }
}

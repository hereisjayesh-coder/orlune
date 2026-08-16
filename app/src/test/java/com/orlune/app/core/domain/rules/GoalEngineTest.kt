package com.orlune.app.core.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Test

class GoalEngineTest {

    @Test
    fun `zero planned units returns zero progress`() {
        assertEquals(0.0, GoalEngine.progress(completedUnits = 10, plannedUnits = 0), 0.0001)
    }

    @Test
    fun `negative planned units returns zero progress`() {
        assertEquals(0.0, GoalEngine.progress(completedUnits = 10, plannedUnits = -5), 0.0001)
    }

    @Test
    fun `partial completion returns a fractional ratio`() {
        assertEquals(0.5, GoalEngine.progress(completedUnits = 30, plannedUnits = 60), 0.0001)
    }

    @Test
    fun `exact completion returns one`() {
        assertEquals(1.0, GoalEngine.progress(completedUnits = 60, plannedUnits = 60), 0.0001)
    }

    @Test
    fun `over-completion returns a ratio above one, not clamped`() {
        assertEquals(1.5, GoalEngine.progress(completedUnits = 90, plannedUnits = 60), 0.0001)
    }

    @Test
    fun `zero completed units returns zero progress`() {
        assertEquals(0.0, GoalEngine.progress(completedUnits = 0, plannedUnits = 60), 0.0001)
    }
}

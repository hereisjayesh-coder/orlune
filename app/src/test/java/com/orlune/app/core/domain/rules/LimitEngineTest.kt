package com.orlune.app.core.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Test

class LimitEngineTest {

    @Test
    fun `usage well under threshold is under limit`() {
        assertEquals(LimitState.UNDER_LIMIT, LimitEngine.evaluate(currentUsageSeconds = 100, thresholdSeconds = 3600))
    }

    @Test
    fun `usage exactly at threshold is over limit`() {
        assertEquals(LimitState.AT_OR_OVER_LIMIT, LimitEngine.evaluate(currentUsageSeconds = 3600, thresholdSeconds = 3600))
    }

    @Test
    fun `usage over threshold is over limit`() {
        assertEquals(LimitState.AT_OR_OVER_LIMIT, LimitEngine.evaluate(currentUsageSeconds = 4000, thresholdSeconds = 3600))
    }

    @Test
    fun `zero threshold is always over limit`() {
        assertEquals(LimitState.AT_OR_OVER_LIMIT, LimitEngine.evaluate(currentUsageSeconds = 0, thresholdSeconds = 0))
    }

    @Test
    fun `negative threshold is always over limit`() {
        assertEquals(LimitState.AT_OR_OVER_LIMIT, LimitEngine.evaluate(currentUsageSeconds = 0, thresholdSeconds = -60))
    }

    @Test
    fun `zero usage under a positive threshold is under limit`() {
        assertEquals(LimitState.UNDER_LIMIT, LimitEngine.evaluate(currentUsageSeconds = 0, thresholdSeconds = 60))
    }
}

package com.orlune.app.core.domain.usage

import org.junit.Assert.assertEquals
import org.junit.Test

class UsageAggregatorTest {

    @Test
    fun `empty session list aggregates to zero`() {
        val totals = UsageAggregator.aggregate(emptyList())

        assertEquals(0L, totals.totalUsageSeconds)
        assertEquals(0, totals.launchCount)
        assertEquals(0, totals.sessionCount)
    }

    @Test
    fun `single session aggregates to its own duration`() {
        val sessions = listOf(ClosedSession("app.a", startTs = 0L, endTs = 90_000L)) // 90s

        val totals = UsageAggregator.aggregate(sessions)

        assertEquals(90L, totals.totalUsageSeconds)
        assertEquals(1, totals.launchCount)
        assertEquals(1, totals.sessionCount)
    }

    @Test
    fun `multiple sessions sum durations and count each session`() {
        val sessions = listOf(
            ClosedSession("app.a", startTs = 0L, endTs = 60_000L),      // 60s
            ClosedSession("app.a", startTs = 120_000L, endTs = 150_000L) // 30s
        )

        val totals = UsageAggregator.aggregate(sessions)

        assertEquals(90L, totals.totalUsageSeconds)
        assertEquals(2, totals.launchCount)
        assertEquals(2, totals.sessionCount)
    }

    @Test
    fun `sub-second durations truncate rather than round`() {
        val sessions = listOf(ClosedSession("app.a", startTs = 0L, endTs = 1_999L)) // 1.999s

        val totals = UsageAggregator.aggregate(sessions)

        assertEquals(1L, totals.totalUsageSeconds)
    }
}

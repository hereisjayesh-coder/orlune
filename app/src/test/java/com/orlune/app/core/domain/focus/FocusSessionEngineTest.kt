package com.orlune.app.core.domain.focus

import com.orlune.app.data.local.entity.FocusSessionEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class FocusSessionEngineTest {

    private fun session(
        startTs: Long,
        plannedMinutes: Int = 25,
        endTs: Long? = null,
        completedMinutes: Int = 0,
        blockedPackages: String = "app.a"
    ) = FocusSessionEntity(
        startTs = startTs,
        endTs = endTs,
        plannedMinutes = plannedMinutes,
        completedMinutes = completedMinutes,
        blockedCategoryIds = "",
        blockedPackages = blockedPackages
    )

    // --- stateOf ---

    @Test
    fun `a session starting in the future is SCHEDULED`() {
        val s = session(startTs = 10_000)
        assertEquals(FocusSessionState.SCHEDULED, FocusSessionEngine.stateOf(s, nowMillis = 5_000))
    }

    @Test
    fun `a started, not-yet-ended session is ACTIVE`() {
        val s = session(startTs = 5_000)
        assertEquals(FocusSessionState.ACTIVE, FocusSessionEngine.stateOf(s, nowMillis = 10_000))
    }

    @Test
    fun `a session left ACTIVE long past its planned end is still ACTIVE until reconciled (restart safety)`() {
        val s = session(startTs = 0, plannedMinutes = 5, endTs = null, completedMinutes = 0)
        // Process was "dead" for an hour past the planned 5-minute end.
        assertEquals(FocusSessionState.ACTIVE, FocusSessionEngine.stateOf(s, nowMillis = 3_600_000))
    }

    @Test
    fun `an ended session that reached its planned duration is COMPLETED`() {
        val s = session(startTs = 0, plannedMinutes = 25, endTs = 1_500_000, completedMinutes = 25)
        assertEquals(FocusSessionState.COMPLETED, FocusSessionEngine.stateOf(s, nowMillis = 2_000_000))
    }

    @Test
    fun `an ended session that stopped short is INTERRUPTED`() {
        val s = session(startTs = 0, plannedMinutes = 25, endTs = 300_000, completedMinutes = 5)
        assertEquals(FocusSessionState.INTERRUPTED, FocusSessionEngine.stateOf(s, nowMillis = 2_000_000))
    }

    // --- tick ---

    @Test
    fun `tick under the planned duration reports elapsed minutes and stays open`() {
        val s = session(startTs = 0, plannedMinutes = 25)
        val update = FocusSessionEngine.tick(s, nowMillis = 10 * 60_000L) // 10 minutes in

        assertEquals(10, update.completedMinutes)
        assertEquals(null, update.endTs)
    }

    @Test
    fun `tick exactly at the planned duration finalizes`() {
        val s = session(startTs = 0, plannedMinutes = 25)
        val update = FocusSessionEngine.tick(s, nowMillis = 25 * 60_000L)

        assertEquals(25, update.completedMinutes)
        assertEquals(25 * 60_000L, update.endTs)
    }

    @Test
    fun `tick well past the planned duration still finalizes at the exact planned instant`() {
        val s = session(startTs = 0, plannedMinutes = 25)
        // Simulates a restart that missed several ticks.
        val update = FocusSessionEngine.tick(s, nowMillis = 3_600_000L)

        assertEquals(25, update.completedMinutes)
        assertEquals(25 * 60_000L, update.endTs)
    }

    // --- interrupt ---

    @Test
    fun `interrupt mid-session ends it with the elapsed minutes so far`() {
        val s = session(startTs = 0, plannedMinutes = 25)
        val update = FocusSessionEngine.interrupt(s, nowMillis = 7 * 60_000L)

        assertEquals(7, update.completedMinutes)
        assertEquals(7 * 60_000L, update.endTs)
    }

    @Test
    fun `interrupt never reports more than the planned minutes`() {
        val s = session(startTs = 0, plannedMinutes = 25)
        val update = FocusSessionEngine.interrupt(s, nowMillis = 60 * 60_000L)

        assertEquals(25, update.completedMinutes)
    }

    // --- blockedPackages ---

    @Test
    fun `blockedPackages parses a single package`() {
        val s = session(startTs = 0, blockedPackages = "app.a")
        assertEquals(setOf("app.a"), FocusSessionEngine.blockedPackages(s))
    }

    @Test
    fun `blockedPackages parses multiple packages and trims whitespace`() {
        val s = session(startTs = 0, blockedPackages = "app.a, app.b ,app.c")
        assertEquals(setOf("app.a", "app.b", "app.c"), FocusSessionEngine.blockedPackages(s))
    }

    @Test
    fun `blockedPackages is empty for an empty string`() {
        val s = session(startTs = 0, blockedPackages = "")
        assertEquals(emptySet<String>(), FocusSessionEngine.blockedPackages(s))
    }
}

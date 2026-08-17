package com.orlune.app.core.domain.focus

import com.orlune.app.data.local.entity.FocusSessionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusNotificationPolicyTest {

    private fun session(
        id: Long,
        startTs: Long,
        endTs: Long? = null,
        plannedMinutes: Int = 25,
        completedMinutes: Int = 0,
        notificationPolicy: String = "ALLOW_ALL",
        allowedNotificationPackages: String = ""
    ) = FocusSessionEntity(
        id = id,
        startTs = startTs,
        endTs = endTs,
        plannedMinutes = plannedMinutes,
        completedMinutes = completedMinutes,
        blockedCategoryIds = "",
        blockedPackages = "app.a",
        notificationPolicy = notificationPolicy,
        allowedNotificationPackages = allowedNotificationPackages
    )

    // --- policy selection: restrictiveness ordering ---

    @Test
    fun `restrictiveness ranks SILENCE_ALL as most restrictive and ALLOW_ALL as least`() {
        val ordered = FocusNotificationPolicy.entries.sortedBy { it.restrictiveness }
        assertEquals(
            listOf(
                FocusNotificationPolicy.ALLOW_ALL,
                FocusNotificationPolicy.ALLOW_CALLS_AND_SELECTED,
                FocusNotificationPolicy.ALLOW_CALLS,
                FocusNotificationPolicy.SILENCE_ALL
            ),
            ordered
        )
    }

    @Test
    fun `fromStored falls back to ALLOW_ALL for an unrecognized value rather than crashing or blocking`() {
        assertEquals(FocusNotificationPolicy.ALLOW_ALL, FocusNotificationPolicy.fromStored("not-a-real-policy"))
        assertEquals(FocusNotificationPolicy.ALLOW_ALL, FocusNotificationPolicy.fromStored(""))
    }

    @Test
    fun `fromStored round-trips every real enum value`() {
        FocusNotificationPolicy.entries.forEach {
            assertEquals(it, FocusNotificationPolicy.fromStored(it.name))
        }
    }

    // --- toZenSpec mapping ---

    @Test
    fun `ALLOW_ALL maps to no Zen spec at all`() {
        assertNull(FocusNotificationPolicy.ALLOW_ALL.toZenSpec())
    }

    @Test
    fun `SILENCE_ALL allows nothing through`() {
        val spec = FocusNotificationPolicy.SILENCE_ALL.toZenSpec()!!
        assertEquals(FocusZenSpec(allowCalls = false, allowRepeatCallers = false, allowSelectedApps = false), spec)
    }

    @Test
    fun `ALLOW_CALLS allows calls and repeat callers but not selected apps`() {
        val spec = FocusNotificationPolicy.ALLOW_CALLS.toZenSpec()!!
        assertEquals(FocusZenSpec(allowCalls = true, allowRepeatCallers = true, allowSelectedApps = false), spec)
    }

    @Test
    fun `ALLOW_CALLS_AND_SELECTED allows calls, repeat callers, and selected apps`() {
        val spec = FocusNotificationPolicy.ALLOW_CALLS_AND_SELECTED.toZenSpec()!!
        assertEquals(FocusZenSpec(allowCalls = true, allowRepeatCallers = true, allowSelectedApps = true), spec)
    }

    // --- allowedNotificationPackages() parsing ---

    @Test
    fun `allowedNotificationPackages parses, trims, and drops empties same as blockedPackages`() {
        val s = session(1, 0, notificationPolicy = "ALLOW_CALLS_AND_SELECTED", allowedNotificationPackages = " app.a ,app.b,,app.a")
        assertEquals(setOf("app.a", "app.b"), s.allowedNotificationPackages())
    }

    @Test
    fun `allowedNotificationPackages is empty for a blank stored value`() {
        assertEquals(emptySet<String>(), session(1, 0).allowedNotificationPackages())
    }

    // --- effectiveFocusNotificationState: state transitions / overlapping sessions ---

    @Test
    fun `no sessions means no effective state`() {
        assertNull(effectiveFocusNotificationState(emptyList(), nowMillis = 1_000L))
    }

    @Test
    fun `a single ACTIVE ALLOW_ALL session means no effective state`() {
        val s = session(1, startTs = 0, notificationPolicy = "ALLOW_ALL")
        assertNull(effectiveFocusNotificationState(listOf(s), nowMillis = 1_000L))
    }

    @Test
    fun `a SCHEDULED (future) session is not counted as active`() {
        val s = session(1, startTs = 10_000L, notificationPolicy = "SILENCE_ALL")
        assertNull(effectiveFocusNotificationState(listOf(s), nowMillis = 0L))
    }

    @Test
    fun `a COMPLETED (already ended) session is not counted as active`() {
        val s = session(1, startTs = 0, endTs = 500L, plannedMinutes = 1, completedMinutes = 1, notificationPolicy = "SILENCE_ALL")
        assertNull(effectiveFocusNotificationState(listOf(s), nowMillis = 100_000L))
    }

    @Test
    fun `a single ACTIVE SILENCE_ALL session resolves to SILENCE_ALL`() {
        val s = session(1, startTs = 0, notificationPolicy = "SILENCE_ALL")
        val result = effectiveFocusNotificationState(listOf(s), nowMillis = 1_000L)
        assertEquals(FocusNotificationPolicy.SILENCE_ALL, result?.policy)
        assertEquals(emptySet<String>(), result?.allowedPackages)
    }

    @Test
    fun `overlapping active sessions resolve to the most restrictive of the two`() {
        val allowAll = session(1, startTs = 0, notificationPolicy = "ALLOW_ALL")
        val silenceAll = session(2, startTs = 0, notificationPolicy = "SILENCE_ALL")
        val result = effectiveFocusNotificationState(listOf(allowAll, silenceAll), nowMillis = 1_000L)
        assertEquals(FocusNotificationPolicy.SILENCE_ALL, result?.policy)
    }

    @Test
    fun `overlapping ALLOW_CALLS and ALLOW_CALLS_AND_SELECTED resolve to the more restrictive ALLOW_CALLS`() {
        val allowCalls = session(1, startTs = 0, notificationPolicy = "ALLOW_CALLS")
        val allowCallsAndSelected = session(2, startTs = 0, notificationPolicy = "ALLOW_CALLS_AND_SELECTED", allowedNotificationPackages = "app.chat")
        val result = effectiveFocusNotificationState(listOf(allowCalls, allowCallsAndSelected), nowMillis = 1_000L)
        assertEquals(FocusNotificationPolicy.ALLOW_CALLS, result?.policy)
        assertEquals(emptySet<String>(), result?.allowedPackages)
    }

    @Test
    fun `overlapping ALLOW_CALLS_AND_SELECTED sessions union their allowed packages`() {
        val a = session(1, startTs = 0, notificationPolicy = "ALLOW_CALLS_AND_SELECTED", allowedNotificationPackages = "app.chat")
        val b = session(2, startTs = 0, notificationPolicy = "ALLOW_CALLS_AND_SELECTED", allowedNotificationPackages = "app.mail,app.chat")
        val result = effectiveFocusNotificationState(listOf(a, b), nowMillis = 1_000L)
        assertEquals(FocusNotificationPolicy.ALLOW_CALLS_AND_SELECTED, result?.policy)
        assertEquals(setOf("app.chat", "app.mail"), result?.allowedPackages)
    }

    @Test
    fun `a more permissive session's allowed packages never leak in when a stricter policy wins`() {
        // ALLOW_ALL has no package selection concept, but even a hypothetical stray
        // value in its column must never surface once SILENCE_ALL wins.
        val allowAll = session(1, startTs = 0, notificationPolicy = "ALLOW_ALL", allowedNotificationPackages = "app.should.not.appear")
        val silenceAll = session(2, startTs = 0, notificationPolicy = "SILENCE_ALL")
        val result = effectiveFocusNotificationState(listOf(allowAll, silenceAll), nowMillis = 1_000L)
        assertEquals(FocusNotificationPolicy.SILENCE_ALL, result?.policy)
        assertTrue(result?.allowedPackages?.isEmpty() == true)
    }

    @Test
    fun `an unrecognized stored policy value is treated as ALLOW_ALL, never as silencing`() {
        val malformed = session(1, startTs = 0, notificationPolicy = "corrupt-value")
        assertNull(effectiveFocusNotificationState(listOf(malformed), nowMillis = 1_000L))
    }

    @Test
    fun `cancellation - an INTERRUPTED session no longer contributes once ended`() {
        val s = session(1, startTs = 0, endTs = 100L, plannedMinutes = 25, completedMinutes = 1, notificationPolicy = "SILENCE_ALL")
        assertNull(effectiveFocusNotificationState(listOf(s), nowMillis = 100L))
    }
}

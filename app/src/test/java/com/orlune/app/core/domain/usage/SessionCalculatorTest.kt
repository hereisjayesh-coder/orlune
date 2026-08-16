package com.orlune.app.core.domain.usage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class SessionCalculatorTest {

    private val zone: ZoneId = ZoneOffset.UTC
    private val calculator = SessionCalculator(zone)

    private fun ts(dateTime: String): Long =
        LocalDateTime.parse(dateTime).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun `simple foreground-background pair closes one session`() {
        val events = listOf(
            RawUsageEvent("app.a", UsageEventType.FOREGROUND, ts("2026-08-16T10:00:00")),
            RawUsageEvent("app.a", UsageEventType.BACKGROUND, ts("2026-08-16T10:05:00"))
        )

        val result = calculator.process(events)

        assertEquals(1, result.closedSessions.size)
        assertEquals("app.a", result.closedSessions[0].packageName)
        assertEquals(ts("2026-08-16T10:00:00"), result.closedSessions[0].startTs)
        assertEquals(ts("2026-08-16T10:05:00"), result.closedSessions[0].endTs)
        assertTrue(result.stillOpen.isEmpty())
    }

    @Test
    fun `trailing foreground with no background stays open`() {
        val events = listOf(
            RawUsageEvent("app.a", UsageEventType.FOREGROUND, ts("2026-08-16T10:00:00"))
        )

        val result = calculator.process(events)

        assertTrue(result.closedSessions.isEmpty())
        assertEquals(mapOf("app.a" to ts("2026-08-16T10:00:00")), result.stillOpen)
    }

    @Test
    fun `existing open session from a previous run gets closed by a background event`() {
        val existingOpen = mapOf("app.a" to ts("2026-08-16T09:55:00"))
        val events = listOf(
            RawUsageEvent("app.a", UsageEventType.BACKGROUND, ts("2026-08-16T10:00:00"))
        )

        val result = calculator.process(events, existingOpen)

        assertEquals(1, result.closedSessions.size)
        assertEquals(ts("2026-08-16T09:55:00"), result.closedSessions[0].startTs)
        assertEquals(ts("2026-08-16T10:00:00"), result.closedSessions[0].endTs)
        assertTrue(result.stillOpen.isEmpty())
    }

    @Test
    fun `existing open session for a different package is implicitly closed when another app comes to foreground`() {
        val existingOpen = mapOf("app.a" to ts("2026-08-16T09:55:00"))
        val events = listOf(
            RawUsageEvent("app.b", UsageEventType.FOREGROUND, ts("2026-08-16T10:00:00")),
            RawUsageEvent("app.b", UsageEventType.BACKGROUND, ts("2026-08-16T10:05:00"))
        )

        val result = calculator.process(events, existingOpen)

        assertEquals(2, result.closedSessions.size)
        val appAClose = result.closedSessions.first { it.packageName == "app.a" }
        assertEquals(ts("2026-08-16T09:55:00"), appAClose.startTs)
        assertEquals(ts("2026-08-16T10:00:00"), appAClose.endTs)
    }

    @Test
    fun `new foreground implicitly closes a different still-open package`() {
        val events = listOf(
            RawUsageEvent("app.a", UsageEventType.FOREGROUND, ts("2026-08-16T10:00:00")),
            RawUsageEvent("app.b", UsageEventType.FOREGROUND, ts("2026-08-16T10:03:00"))
        )

        val result = calculator.process(events)

        assertEquals(1, result.closedSessions.size)
        assertEquals("app.a", result.closedSessions[0].packageName)
        assertEquals(ts("2026-08-16T10:00:00"), result.closedSessions[0].startTs)
        assertEquals(ts("2026-08-16T10:03:00"), result.closedSessions[0].endTs)
        assertEquals(mapOf("app.b" to ts("2026-08-16T10:03:00")), result.stillOpen)
    }

    @Test
    fun `device shutdown closes every open session at shutdown time`() {
        val existingOpen = mapOf("app.a" to ts("2026-08-16T09:00:00"), "app.b" to ts("2026-08-16T09:30:00"))
        val events = listOf(
            RawUsageEvent("system", UsageEventType.DEVICE_SHUTDOWN, ts("2026-08-16T09:45:00"))
        )

        val result = calculator.process(events, existingOpen)

        assertEquals(2, result.closedSessions.size)
        assertTrue(result.closedSessions.all { it.endTs == ts("2026-08-16T09:45:00") })
        assertTrue(result.stillOpen.isEmpty())
    }

    @Test
    fun `duplicate foreground event does not reset the original start time`() {
        val events = listOf(
            RawUsageEvent("app.a", UsageEventType.FOREGROUND, ts("2026-08-16T10:00:00")),
            RawUsageEvent("app.a", UsageEventType.FOREGROUND, ts("2026-08-16T10:02:00")),
            RawUsageEvent("app.a", UsageEventType.BACKGROUND, ts("2026-08-16T10:05:00"))
        )

        val result = calculator.process(events)

        assertEquals(1, result.closedSessions.size)
        assertEquals(ts("2026-08-16T10:00:00"), result.closedSessions[0].startTs)
    }

    @Test
    fun `background event with no matching open session is ignored`() {
        val events = listOf(
            RawUsageEvent("app.a", UsageEventType.BACKGROUND, ts("2026-08-16T10:00:00"))
        )

        val result = calculator.process(events)

        assertTrue(result.closedSessions.isEmpty())
        assertTrue(result.stillOpen.isEmpty())
    }

    @Test
    fun `session spanning local midnight is split into two day-clamped segments`() {
        val events = listOf(
            RawUsageEvent("app.a", UsageEventType.FOREGROUND, ts("2026-08-16T23:50:00")),
            RawUsageEvent("app.a", UsageEventType.BACKGROUND, ts("2026-08-17T00:10:00"))
        )

        val result = calculator.process(events)

        assertEquals(2, result.closedSessions.size)
        val first = result.closedSessions[0]
        val second = result.closedSessions[1]
        assertEquals(ts("2026-08-16T23:50:00"), first.startTs)
        assertEquals(ts("2026-08-17T00:00:00"), first.endTs)
        assertEquals(ts("2026-08-17T00:00:00"), second.startTs)
        assertEquals(ts("2026-08-17T00:10:00"), second.endTs)
    }

    @Test
    fun `session spanning multiple midnights splits into one segment per day`() {
        val events = listOf(
            RawUsageEvent("app.a", UsageEventType.FOREGROUND, ts("2026-08-16T23:50:00")),
            RawUsageEvent("app.a", UsageEventType.BACKGROUND, ts("2026-08-19T00:10:00"))
        )

        val result = calculator.process(events)

        assertEquals(4, result.closedSessions.size)
        assertEquals(ts("2026-08-16T23:50:00"), result.closedSessions.first().startTs)
        assertEquals(ts("2026-08-19T00:10:00"), result.closedSessions.last().endTs)
        // Every segment boundary lands exactly on a local midnight.
        for (i in 0 until result.closedSessions.size - 1) {
            assertEquals(result.closedSessions[i].endTs, result.closedSessions[i + 1].startTs)
        }
    }

    @Test
    fun `zero-length pairing is dropped`() {
        val events = listOf(
            RawUsageEvent("app.a", UsageEventType.FOREGROUND, ts("2026-08-16T10:00:00")),
            RawUsageEvent("app.a", UsageEventType.BACKGROUND, ts("2026-08-16T10:00:00"))
        )

        val result = calculator.process(events)

        assertTrue(result.closedSessions.isEmpty())
        assertTrue(result.stillOpen.isEmpty())
    }
}

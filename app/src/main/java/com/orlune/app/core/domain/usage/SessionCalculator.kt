package com.orlune.app.core.domain.usage

import java.time.Instant
import java.time.ZoneId

data class ClosedSession(
    val packageName: String,
    val startTs: Long,
    val endTs: Long
)

data class SessionProcessingResult(
    val closedSessions: List<ClosedSession>,
    /** packageName -> startTs, for sessions still open after processing this batch. */
    val stillOpen: Map<String, Long>
)

/**
 * Pairs raw foreground/background events into sessions, per
 * `docs/phase-0-research.md` Section 9 (SessionCalculator: "pair MOVE_TO_FOREGROUND/
 * BACKGROUND events, clamp orphaned sessions at day rollover").
 *
 * Edge cases handled:
 * - [events] must already be sorted by timestamp ascending (what
 *   `UsageStatsManager.queryEvents` returns).
 * - [existingOpenSessions] carries forward sessions left open by a previous run
 *   (e.g. whatever app was in the foreground when this batch was queried), so a
 *   session is never lost or double-counted across processing runs.
 * - A FOREGROUND event for an already-open package is a no-op (duplicate/redundant
 *   event) rather than resetting the start time.
 * - A FOREGROUND event for package B while other packages are still open implicitly
 *   closes all of them at B's timestamp — defends against missed BACKGROUND events,
 *   since only one app is genuinely foreground at a time.
 * - A BACKGROUND event for a package that isn't open is ignored (its matching
 *   FOREGROUND was outside the queried window, already handled by a prior run).
 * - DEVICE_SHUTDOWN closes every currently-open session at that timestamp — there's
 *   no guaranteed BACKGROUND event when the OS shuts down.
 * - Every closed session is split at local-midnight boundaries (per [zoneId]) before
 *   being returned, so a session never spans more than one calendar day — this is
 *   what lets DailyUsage aggregation simply sum sessions per day. Day boundaries are
 *   computed at processing time using the current default zone, so a timezone change
 *   affects only sessions processed after the change, not historical data.
 * - A zero-or-negative-length pairing (e.g. clock moved backward between events) is
 *   dropped rather than recorded as a negative-duration session.
 */
class SessionCalculator(private val zoneId: ZoneId = ZoneId.systemDefault()) {

    fun process(
        events: List<RawUsageEvent>,
        existingOpenSessions: Map<String, Long> = emptyMap()
    ): SessionProcessingResult {
        val open = existingOpenSessions.toMutableMap()
        val closed = mutableListOf<ClosedSession>()

        for (event in events) {
            when (event.type) {
                UsageEventType.FOREGROUND -> {
                    val stalePackages = open.keys.filter { it != event.packageName }
                    for (packageName in stalePackages) {
                        closeSession(packageName, open.remove(packageName)!!, event.timestamp, closed)
                    }
                    open.putIfAbsent(event.packageName, event.timestamp)
                }

                UsageEventType.BACKGROUND -> {
                    val startTs = open.remove(event.packageName)
                    if (startTs != null) {
                        closeSession(event.packageName, startTs, event.timestamp, closed)
                    }
                }

                UsageEventType.DEVICE_SHUTDOWN -> {
                    val snapshot = open.toMap()
                    open.clear()
                    for ((packageName, startTs) in snapshot) {
                        closeSession(packageName, startTs, event.timestamp, closed)
                    }
                }

                UsageEventType.OTHER -> Unit
            }
        }

        return SessionProcessingResult(closedSessions = closed, stillOpen = open)
    }

    private fun closeSession(packageName: String, startTs: Long, endTs: Long, into: MutableList<ClosedSession>) {
        if (endTs <= startTs) return
        into += splitAtDayBoundaries(packageName, startTs, endTs)
    }

    private fun splitAtDayBoundaries(packageName: String, startTs: Long, endTs: Long): List<ClosedSession> {
        val segments = mutableListOf<ClosedSession>()
        var segmentStart = startTs
        while (segmentStart < endTs) {
            val startOfNextDay = Instant.ofEpochMilli(segmentStart)
                .atZone(zoneId)
                .toLocalDate()
                .plusDays(1)
                .atStartOfDay(zoneId)
                .toInstant()
                .toEpochMilli()
            val segmentEnd = minOf(endTs, startOfNextDay)
            segments += ClosedSession(packageName, segmentStart, segmentEnd)
            segmentStart = segmentEnd
        }
        return segments
    }
}

package com.orlune.app.core.domain.usage

/**
 * Aggregates already-day-clamped closed sessions (see [SessionCalculator]) into the
 * per-app-per-day totals `DailyUsageEntity` stores, per `docs/phase-0-research.md`
 * Section 9 (UsageCalculator: "Σ(session durations) per packageName per date").
 */
object UsageAggregator {
    data class DailyTotals(
        val totalUsageSeconds: Long,
        val launchCount: Int,
        val sessionCount: Int
    )

    /**
     * [sessions] must all belong to the same package and calendar day — guaranteed by
     * construction since [SessionCalculator] never returns a session spanning more than
     * one day. `launchCount` and `sessionCount` are equal in this model: Section 8 lists
     * them as separate fields, but nothing yet defines a distinction between a "launch"
     * and a "session" (e.g. deduplicating rapid app-switch flicker), so one isn't
     * invented here without a documented definition to implement against.
     */
    fun aggregate(sessions: List<ClosedSession>): DailyTotals {
        val totalMillis = sessions.sumOf { it.endTs - it.startTs }
        return DailyTotals(
            totalUsageSeconds = totalMillis / 1000,
            launchCount = sessions.size,
            sessionCount = sessions.size
        )
    }
}

package com.orlune.app.core.domain.focus

import com.orlune.app.data.local.entity.FocusSessionEntity

enum class FocusSessionState { SCHEDULED, ACTIVE, COMPLETED, INTERRUPTED }

data class FocusSessionUpdate(val completedMinutes: Int, val endTs: Long?)

/**
 * Pure focus-session lifecycle logic, per `docs/phase-0-research.md` Section 14
 * Phase 6. No stored status column — [stateOf] derives state from
 * [FocusSessionEntity.startTs]/[FocusSessionEntity.endTs]/[FocusSessionEntity.plannedMinutes]/
 * [FocusSessionEntity.completedMinutes] plus the current time, so a crash, reboot, or
 * app restart can never leave a session in a state that doesn't reconcile correctly
 * the next time something reads it — there's nothing to get out of sync.
 */
object FocusSessionEngine {

    /**
     * - Not yet started ([FocusSessionEntity.startTs] in the future) -> [FocusSessionState.SCHEDULED]
     * - Started, not yet ended -> [FocusSessionState.ACTIVE] (covers a session left
     *   open past its planned end while the process wasn't running to close it —
     *   the caller's next [tick] finalizes it, this function alone doesn't lie about
     *   an unfinalized row)
     * - Ended, reached its planned duration -> [FocusSessionState.COMPLETED]
     * - Ended before reaching its planned duration -> [FocusSessionState.INTERRUPTED]
     */
    fun stateOf(session: FocusSessionEntity, nowMillis: Long): FocusSessionState {
        if (session.endTs != null) {
            return if (session.completedMinutes >= session.plannedMinutes) {
                FocusSessionState.COMPLETED
            } else {
                FocusSessionState.INTERRUPTED
            }
        }
        return if (nowMillis < session.startTs) FocusSessionState.SCHEDULED else FocusSessionState.ACTIVE
    }

    fun blockedPackages(session: FocusSessionEntity): Set<String> =
        session.blockedPackages.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()

    /**
     * Only meaningful for a session currently [FocusSessionState.ACTIVE]. Finalizes at
     * the *exact* planned instant (`startTs + plannedMinutes`), not "now" — so natural
     * completion is deterministic regardless of how late the caller's next poll
     * happens to run (e.g. after a restart that missed several ticks).
     */
    fun tick(session: FocusSessionEntity, nowMillis: Long): FocusSessionUpdate {
        val plannedEndTs = session.startTs + session.plannedMinutes * MILLIS_PER_MINUTE
        return if (nowMillis >= plannedEndTs) {
            FocusSessionUpdate(completedMinutes = session.plannedMinutes, endTs = plannedEndTs)
        } else {
            val elapsedMinutes = ((nowMillis - session.startTs).coerceAtLeast(0) / MILLIS_PER_MINUTE).toInt()
            FocusSessionUpdate(completedMinutes = elapsedMinutes.coerceAtMost(session.plannedMinutes), endTs = null)
        }
    }

    /** A manual "Stop" action — ends the session now, short of its planned duration (unless the timing happens to land exactly on it). */
    fun interrupt(session: FocusSessionEntity, nowMillis: Long): FocusSessionUpdate {
        val elapsedMinutes = ((nowMillis - session.startTs).coerceAtLeast(0) / MILLIS_PER_MINUTE).toInt()
        return FocusSessionUpdate(completedMinutes = elapsedMinutes.coerceAtMost(session.plannedMinutes), endTs = nowMillis)
    }

    private const val MILLIS_PER_MINUTE = 60_000L
}

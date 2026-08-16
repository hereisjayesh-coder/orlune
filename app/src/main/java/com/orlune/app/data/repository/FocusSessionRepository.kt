package com.orlune.app.data.repository

import com.orlune.app.core.domain.focus.FocusSessionEngine
import com.orlune.app.core.domain.focus.FocusSessionState
import com.orlune.app.data.local.dao.FocusSessionDao
import com.orlune.app.data.local.entity.FocusSessionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Owns focus-session persistence and lifecycle reconciliation, per
 * `docs/phase-0-research.md` Section 14 Phase 6. Mirrors [UsageRepository]'s shape —
 * constructor-injected DAO, no dispatcher-switching (the caller runs on a background
 * dispatcher already, same as [UsageRepository.processNewEvents]).
 */
class FocusSessionRepository(
    private val focusSessionDao: FocusSessionDao,
    private val nowMillis: () -> Long = System::currentTimeMillis
) {
    fun observeAll(): Flow<List<FocusSessionEntity>> = focusSessionDao.observeAll()

    suspend fun startSession(plannedMinutes: Int, blockedPackages: List<String>, startAt: Long = nowMillis()): Long {
        require(plannedMinutes in MIN_PLANNED_MINUTES..MAX_PLANNED_MINUTES) {
            "plannedMinutes must be between $MIN_PLANNED_MINUTES and $MAX_PLANNED_MINUTES"
        }
        val normalizedPackages = blockedPackages
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()
        require(normalizedPackages.isNotEmpty()) { "blockedPackages must contain at least one package" }

        return focusSessionDao.upsert(
            FocusSessionEntity(
                startTs = startAt,
                endTs = null,
                plannedMinutes = plannedMinutes,
                completedMinutes = 0,
                blockedCategoryIds = "",
                blockedPackages = normalizedPackages.joinToString(",")
            )
        )
    }

    /** Ends every currently-[FocusSessionState.ACTIVE] session now, short of its planned duration. */
    suspend fun cancelActiveSessions() {
        val now = nowMillis()
        val sessions = focusSessionDao.observeAll().first()
        for (session in sessions) {
            if (FocusSessionEngine.stateOf(session, now) == FocusSessionState.ACTIVE) {
                val update = FocusSessionEngine.interrupt(session, now)
                focusSessionDao.upsert(session.copy(completedMinutes = update.completedMinutes, endTs = update.endTs))
            }
        }
    }

    /**
     * Called every [com.orlune.app.platform.blocking.BlockingMonitorService] tick:
     * advances `completedMinutes` for every currently-active session and finalizes
     * any that have reached their planned duration — including one left `ACTIVE`
     * past its planned end because the process wasn't running for a while (restart/
     * reboot safety), since [FocusSessionEngine.tick] finalizes at the exact planned
     * instant regardless of how late this runs.
     */
    suspend fun reconcileActiveSessions() {
        val now = nowMillis()
        val sessions = focusSessionDao.observeAll().first()
        for (session in sessions) {
            if (FocusSessionEngine.stateOf(session, now) == FocusSessionState.ACTIVE) {
                val update = FocusSessionEngine.tick(session, now)
                if (update.completedMinutes != session.completedMinutes || update.endTs != session.endTs) {
                    focusSessionDao.upsert(session.copy(completedMinutes = update.completedMinutes, endTs = update.endTs))
                }
            }
        }
    }

    private companion object {
        const val MIN_PLANNED_MINUTES = 1
        const val MAX_PLANNED_MINUTES = 24 * 60
    }
}

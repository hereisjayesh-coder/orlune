package com.orlune.app.data.repository

import com.orlune.app.core.domain.rules.BlockDecision
import com.orlune.app.core.domain.rules.BlockingEngine
import com.orlune.app.core.domain.rules.LimitEngine
import com.orlune.app.core.domain.rules.LimitState
import com.orlune.app.core.domain.rules.ScheduleEngine
import com.orlune.app.data.local.dao.AppListEntryDao
import com.orlune.app.data.local.dao.DailyUsageDao
import com.orlune.app.data.local.dao.RuleDao
import com.orlune.app.data.local.dao.ScheduleDao
import com.orlune.app.data.local.dao.SessionDao
import com.orlune.app.data.local.entity.RuleEntity
import com.orlune.app.data.local.entity.SessionEntity
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Evaluates whether the current foreground app should be blocked, per
 * `docs/phase-0-research.md` Section 1's platform -> rule engine -> decision flow.
 * Dispatches [RuleEntity.type] ("limit"/"schedule" — see its own KDoc) as a plain
 * string, not an enum: nothing in `core/domain` consumes this field directly, so
 * giving it a type here (the only consumer) would be a needless abstraction.
 * All reads use the existing Phase 2 DAOs' Flow-returning queries via [first] for a
 * one-shot snapshot — no new DAO methods, no schema changes.
 *
 * "Current foreground app" is derived from [SessionDao.getOpenSessions] rather than a
 * separate live `UsageStatsManager` poll: an initial implementation queried a short
 * trailing event window directly, but that loses track of any session that's been
 * continuously open longer than the window — exactly the "user kept using the app
 * past the limit" case this repository exists to catch. An open session (no `endTs`
 * yet) *is* "currently foreground," and it's already kept accurate by
 * [UsageRepository.processNewEvents], which the caller runs immediately before this.
 */
class BlockingRepository(
    private val ruleDao: RuleDao,
    private val scheduleDao: ScheduleDao,
    private val appListEntryDao: AppListEntryDao,
    private val dailyUsageDao: DailyUsageDao,
    private val sessionDao: SessionDao,
    private val ownPackageName: String,
    private val zoneId: ZoneId = ZoneId.systemDefault(),
    private val nowMillis: () -> Long = System::currentTimeMillis
) {
    data class Outcome(val packageName: String?, val decision: BlockDecision)

    /**
     * Fails toward [BlockDecision.ALLOW] whenever the target is ambiguous: no open
     * session (nothing currently foreground), or the foreground app is Orlune itself
     * (never blockable, even if a rule somehow targets it).
     */
    suspend fun evaluate(): Outcome {
        val openSessions = sessionDao.getOpenSessions()
        // Normally at most one: SessionCalculator closes any other open session the
        // moment a new FOREGROUND event arrives. Most-recently-started wins if more
        // than one somehow exists, rather than picking arbitrarily.
        val openSession = openSessions.maxByOrNull { it.startTs }
        val packageName = openSession?.packageName

        if (packageName == null || packageName == ownPackageName) {
            return Outcome(packageName, BlockDecision.ALLOW)
        }

        val rules = ruleDao.observeAll().first().filter { it.targetPackageOrCategory == packageName }
        val anyTriggered = rules.any { isTriggered(it, openSession) }

        val listEntries = appListEntryDao.observeByType("block").first() +
            appListEntryDao.observeByType("allow").first()

        return Outcome(packageName, BlockingEngine.decide(packageName, listEntries, anyTriggered))
    }

    /** Malformed/incomplete rule data is treated as not-triggered, never as "block everything." */
    private suspend fun isTriggered(rule: RuleEntity, openSession: SessionEntity): Boolean =
        when (rule.type) {
            RULE_TYPE_LIMIT -> evaluateLimit(rule, openSession)
            RULE_TYPE_SCHEDULE -> evaluateSchedule(rule)
            else -> false
        }

    /**
     * `DailyUsageEntity` only reflects *closed* sessions (Phase 3's aggregation runs
     * when a session closes). Without adding the still-open session's own elapsed
     * time, a single continuous session over the limit would never actually trigger a
     * block until the user backgrounds the app — the opposite of what a live limit
     * check needs. The open session's contribution is clamped to today's portion only
     * (`maxOf(startTs, todayStart)`), so a session that happens to still be open from
     * before local midnight doesn't attribute yesterday's time to today's total.
     */
    private suspend fun evaluateLimit(rule: RuleEntity, openSession: SessionEntity): Boolean {
        val thresholdSeconds = rule.threshold ?: return false
        val epochDay = Instant.ofEpochMilli(nowMillis()).atZone(zoneId).toLocalDate().toEpochDay()
        val aggregatedSeconds = dailyUsageDao.getForAppAndDay(rule.targetPackageOrCategory, epochDay)?.totalUsageSeconds ?: 0L

        val openSessionSeconds = if (openSession.packageName == rule.targetPackageOrCategory) {
            val todayStartMillis = LocalDate.ofEpochDay(epochDay).atStartOfDay(zoneId).toInstant().toEpochMilli()
            val clampedStart = maxOf(openSession.startTs, todayStartMillis)
            (nowMillis() - clampedStart).coerceAtLeast(0) / 1000
        } else {
            0L
        }

        return LimitEngine.evaluate(aggregatedSeconds + openSessionSeconds, thresholdSeconds) == LimitState.AT_OR_OVER_LIMIT
    }

    private suspend fun evaluateSchedule(rule: RuleEntity): Boolean {
        val schedules = scheduleDao.observeForRule(rule.id).first()
        if (schedules.isEmpty()) return false
        val now = LocalDateTime.ofInstant(Instant.ofEpochMilli(nowMillis()), zoneId)
        return schedules.any { ScheduleEngine.isActive(ScheduleEngine.parse(it), now) }
    }

    companion object {
        private const val RULE_TYPE_LIMIT = "limit"
        private const val RULE_TYPE_SCHEDULE = "schedule"
    }
}

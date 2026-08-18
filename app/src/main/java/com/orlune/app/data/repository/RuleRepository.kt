package com.orlune.app.data.repository

import com.orlune.app.core.domain.rules.DailyLimitInput
import com.orlune.app.data.local.dao.RuleDao
import com.orlune.app.data.local.dao.RuleSnoozeDao
import com.orlune.app.data.local.dao.ScheduleDao
import com.orlune.app.data.local.entity.RuleEntity
import com.orlune.app.data.local.entity.RuleSnoozeEntity
import com.orlune.app.data.local.entity.ScheduleEntity

/**
 * Single, validated path for creating/removing a [RuleEntity] (and its associated
 * [ScheduleEntity], for "schedule" rules) and for recording a block-screen snooze —
 * replaces the raw `ruleDao().upsert(RuleEntity(...))` calls that previously existed
 * independently in the onboarding flow and the Limits screen, so a daily-limit
 * threshold is validated identically (via [DailyLimitInput]'s bounds) no matter which
 * screen created it, and so a rule/schedule addition has exactly one place to also
 * trigger [com.orlune.app.platform.blocking.BlockingMonitorService] reconciliation
 * (done by the caller, not here — this class has no `Context`/`Service` dependency,
 * matching every other repository in this package).
 */
class RuleRepository(
    private val ruleDao: RuleDao,
    private val scheduleDao: ScheduleDao,
    private val ruleSnoozeDao: RuleSnoozeDao
) {
    suspend fun addDailyLimit(packageName: String, thresholdSeconds: Long): Long {
        val trimmed = packageName.trim()
        require(trimmed.isNotEmpty()) { "packageName must not be blank" }
        require(thresholdSeconds >= DailyLimitInput.MIN_TOTAL_MINUTES * 60L) {
            "thresholdSeconds must be at least ${DailyLimitInput.MIN_TOTAL_MINUTES} minute(s)"
        }
        require(thresholdSeconds <= DailyLimitInput.MAX_TOTAL_MINUTES * 60L) {
            "thresholdSeconds must be at most ${DailyLimitInput.MAX_TOTAL_MINUTES} minutes"
        }
        return ruleDao.upsert(
            RuleEntity(type = "limit", targetPackageOrCategory = trimmed, threshold = thresholdSeconds, windowDefinition = null)
        )
    }

    suspend fun addSchedule(name: String, packageName: String, daysOfWeek: String, startTime: String, endTime: String): Long {
        val trimmed = packageName.trim()
        require(trimmed.isNotEmpty()) { "packageName must not be blank" }
        val ruleId = ruleDao.upsert(
            RuleEntity(type = "schedule", targetPackageOrCategory = trimmed, threshold = null, windowDefinition = null)
        )
        scheduleDao.upsert(
            ScheduleEntity(name = name, daysOfWeek = daysOfWeek, startTime = startTime, endTime = endTime, associatedRuleId = ruleId)
        )
        return ruleId
    }

    suspend fun delete(rule: RuleEntity) {
        ruleDao.delete(rule)
    }

    /** Records that [packageName] should be allowed for [minutes] more minutes,
     * overriding an otherwise-triggered block — see [RuleSnoozeEntity]'s KDoc for why
     * this never touches the rule/schedule/focus-session data itself. */
    suspend fun snooze(packageName: String, minutes: Int, nowMillis: Long = System.currentTimeMillis()) {
        val trimmed = packageName.trim()
        require(trimmed.isNotEmpty()) { "packageName must not be blank" }
        require(minutes in MIN_SNOOZE_MINUTES..MAX_SNOOZE_MINUTES) {
            "minutes must be between $MIN_SNOOZE_MINUTES and $MAX_SNOOZE_MINUTES"
        }
        ruleSnoozeDao.upsert(RuleSnoozeEntity(packageName = trimmed, snoozedUntil = nowMillis + minutes * 60_000L))
    }

    companion object {
        const val MIN_SNOOZE_MINUTES = 1
        // A generous ceiling, not "unlimited" — the block screen's own Custom stepper
        // offers a narrower practical range; this is the repository-level backstop.
        const val MAX_SNOOZE_MINUTES = 180
    }
}

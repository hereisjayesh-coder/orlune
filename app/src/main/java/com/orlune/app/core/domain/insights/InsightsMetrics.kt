package com.orlune.app.core.domain.insights

import com.orlune.app.data.local.entity.FocusSessionEntity
import com.orlune.app.data.local.entity.RuleEntity

/**
 * Pure, deterministic Insights facts derived entirely from data already stored
 * locally — no AI, no fabricated statistics, same input always produces the same
 * output (matching every other engine under core/domain).
 */
object InsightsMetrics {

    fun focusSessionCount(sessions: List<FocusSessionEntity>, periodStartMillis: Long, periodEndExclusiveMillis: Long): Int =
        sessions.count { it.startTs >= periodStartMillis && it.startTs < periodEndExclusiveMillis }

    fun focusSecondsTotal(sessions: List<FocusSessionEntity>, periodStartMillis: Long, periodEndExclusiveMillis: Long): Long =
        sessions
            .filter { it.startTs >= periodStartMillis && it.startTs < periodEndExclusiveMillis }
            .sumOf { it.completedMinutes * 60L }

    data class LimitCompliance(val compliant: Int, val total: Int)

    /**
     * How many active daily-limit rules are currently at or under their threshold,
     * based on today's tracked usage for each rule's target package. A rule with no
     * usage recorded yet today counts as compliant (nothing to be over the limit on).
     */
    fun limitCompliance(rules: List<RuleEntity>, todaySecondsByPackage: Map<String, Long>): LimitCompliance {
        val limitRules = rules.filter { it.type == "limit" }
        val compliant = limitRules.count { rule ->
            val threshold = rule.threshold ?: return@count true
            (todaySecondsByPackage[rule.targetPackageOrCategory] ?: 0L) <= threshold
        }
        return LimitCompliance(compliant = compliant, total = limitRules.size)
    }
}

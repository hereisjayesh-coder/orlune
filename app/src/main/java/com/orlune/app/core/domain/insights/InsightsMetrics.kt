package com.orlune.app.core.domain.insights

import com.orlune.app.data.local.dao.DayAppUsageRow
import com.orlune.app.data.local.entity.FocusSessionEntity
import com.orlune.app.data.local.entity.RuleEntity
import java.time.LocalDate
import java.time.ZoneId

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

    /** One 7-day bucket of [weeklyBreakdown] — [startEpochDay]..[endEpochDayInclusive]
     * is always exactly 7 days, most recent bucket first (index 0 = the 7 days ending
     * "today"). [ruleDays] is 0 whenever no limit rule ever had usage data in this
     * week — callers should treat that as "not available," not "0 of 0 compliant." */
    data class WeekSummary(
        val startEpochDay: Long,
        val endEpochDayInclusive: Long,
        val totalUsageSeconds: Long,
        val focusSeconds: Long,
        val compliantDays: Int,
        val ruleDays: Int,
        val topApps: List<Pair<String, Long>>
    )

    /**
     * Splits the trailing `weeks * 7` days ending on [today] into consecutive 7-day
     * buckets — entirely a read-time grouping of already-stored [dayRows]
     * ([com.orlune.app.data.local.dao.DailyUsageDao.observeAppDailyUsageBetween]); no
     * new data is stored just to support this view, and nothing here estimates or
     * projects a day that hasn't happened yet. [focusSessions] is the same
     * already-loaded list [focusSecondsTotal] elsewhere in this object already
     * consumes — reused, not re-queried.
     */
    fun weeklyBreakdown(
        dayRows: List<DayAppUsageRow>,
        rules: List<RuleEntity>,
        focusSessions: List<FocusSessionEntity>,
        today: Long,
        weeks: Int,
        zoneId: ZoneId
    ): List<WeekSummary> {
        val limitRules = rules.filter { it.type == "limit" && it.threshold != null }
        return (0 until weeks).map { weekIndex ->
            val end = today - weekIndex * 7L
            val start = end - 6
            val weekRows = dayRows.filter { it.epochDay in start..end }

            val topApps = weekRows.groupBy { it.packageName }
                .mapValues { (_, rows) -> rows.sumOf { it.totalUsageSeconds } }
                .entries
                .sortedByDescending { it.value }
                .take(3)
                .map { it.key to it.value }

            val startMillis = LocalDate.ofEpochDay(start).atStartOfDay(zoneId).toInstant().toEpochMilli()
            val endExclusiveMillis = LocalDate.ofEpochDay(end + 1).atStartOfDay(zoneId).toInstant().toEpochMilli()

            var compliantDays = 0
            var ruleDays = 0
            if (limitRules.isNotEmpty()) {
                for (day in start..end) {
                    val usedByPackage = weekRows.filter { it.epochDay == day }
                        .associate { it.packageName to it.totalUsageSeconds }
                    val dayHasRuleUsage = limitRules.any { it.targetPackageOrCategory in usedByPackage }
                    if (!dayHasRuleUsage) continue
                    ruleDays++
                    val allCompliant = limitRules.all { rule ->
                        (usedByPackage[rule.targetPackageOrCategory] ?: 0L) <= (rule.threshold ?: Long.MAX_VALUE)
                    }
                    if (allCompliant) compliantDays++
                }
            }

            WeekSummary(
                startEpochDay = start,
                endEpochDayInclusive = end,
                totalUsageSeconds = weekRows.sumOf { it.totalUsageSeconds },
                focusSeconds = focusSecondsTotal(focusSessions, startMillis, endExclusiveMillis),
                compliantDays = compliantDays,
                ruleDays = ruleDays,
                topApps = topApps
            )
        }
    }

    /** A single, restrained personality touch (never more than one line) — computed
     * from real comparisons only, never a fabricated streak or score. Null means
     * "nothing notable to say," which is the common case and intentionally silent
     * rather than forcing a highlight every time. */
    fun highlight(thisWeek: WeekSummary, previousWeek: WeekSummary?): String? = when {
        previousWeek != null && thisWeek.totalUsageSeconds < previousWeek.totalUsageSeconds ->
            "🌱 Better week — usage is down from last week."
        thisWeek.ruleDays > 0 && thisWeek.compliantDays == thisWeek.ruleDays ->
            "🔥 Strong consistency — every limit met this week."
        else -> null
    }
}

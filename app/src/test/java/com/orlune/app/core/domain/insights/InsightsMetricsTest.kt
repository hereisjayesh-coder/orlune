package com.orlune.app.core.domain.insights

import com.orlune.app.data.local.dao.DayAppUsageRow
import com.orlune.app.data.local.entity.FocusSessionEntity
import com.orlune.app.data.local.entity.RuleEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneOffset

class InsightsMetricsTest {

    private fun focusSession(startTs: Long, completedMinutes: Int) = FocusSessionEntity(
        startTs = startTs,
        endTs = startTs + completedMinutes * 60_000L,
        plannedMinutes = completedMinutes,
        completedMinutes = completedMinutes,
        blockedCategoryIds = "",
        blockedPackages = "com.example.app"
    )

    @Test
    fun `focusSessionCount only counts sessions that started within the period`() {
        val sessions = listOf(
            focusSession(startTs = 100, completedMinutes = 10), // in period
            focusSession(startTs = 50, completedMinutes = 5),   // before period
            focusSession(startTs = 999, completedMinutes = 5)   // at/after period end (exclusive)
        )
        assertEquals(1, InsightsMetrics.focusSessionCount(sessions, periodStartMillis = 100, periodEndExclusiveMillis = 999))
    }

    @Test
    fun `focusSessionCount is zero for an empty session list`() {
        assertEquals(0, InsightsMetrics.focusSessionCount(emptyList(), periodStartMillis = 0, periodEndExclusiveMillis = 1000))
    }

    @Test
    fun `focusSecondsTotal sums completedMinutes, not plannedMinutes, for sessions in the period`() {
        val sessions = listOf(
            focusSession(startTs = 100, completedMinutes = 10),
            focusSession(startTs = 200, completedMinutes = 20)
        )
        assertEquals(30 * 60L, InsightsMetrics.focusSecondsTotal(sessions, periodStartMillis = 0, periodEndExclusiveMillis = 1000))
    }

    @Test
    fun `focusSecondsTotal excludes sessions outside the period`() {
        val sessions = listOf(
            focusSession(startTs = 100, completedMinutes = 10),
            focusSession(startTs = 5000, completedMinutes = 999)
        )
        assertEquals(10 * 60L, InsightsMetrics.focusSecondsTotal(sessions, periodStartMillis = 0, periodEndExclusiveMillis = 1000))
    }

    private fun limitRule(targetPackage: String, thresholdSeconds: Long?) = RuleEntity(
        type = "limit",
        targetPackageOrCategory = targetPackage,
        threshold = thresholdSeconds,
        windowDefinition = null
    )

    @Test
    fun `limitCompliance counts a rule as compliant when today's usage is under its threshold`() {
        val rules = listOf(limitRule("com.example.a", thresholdSeconds = 3600))
        val usage = mapOf("com.example.a" to 1800L)
        val result = InsightsMetrics.limitCompliance(rules, usage)
        assertEquals(1, result.compliant)
        assertEquals(1, result.total)
    }

    @Test
    fun `limitCompliance counts a rule as non-compliant when today's usage exceeds its threshold`() {
        val rules = listOf(limitRule("com.example.a", thresholdSeconds = 3600))
        val usage = mapOf("com.example.a" to 7200L)
        val result = InsightsMetrics.limitCompliance(rules, usage)
        assertEquals(0, result.compliant)
        assertEquals(1, result.total)
    }

    @Test
    fun `limitCompliance treats usage exactly at the threshold as compliant`() {
        val rules = listOf(limitRule("com.example.a", thresholdSeconds = 3600))
        val usage = mapOf("com.example.a" to 3600L)
        val result = InsightsMetrics.limitCompliance(rules, usage)
        assertEquals(1, result.compliant)
    }

    @Test
    fun `limitCompliance treats a package with no recorded usage today as compliant`() {
        val rules = listOf(limitRule("com.example.a", thresholdSeconds = 3600))
        val result = InsightsMetrics.limitCompliance(rules, emptyMap())
        assertEquals(1, result.compliant)
    }

    @Test
    fun `limitCompliance ignores non-limit rules like schedules`() {
        val rules = listOf(
            limitRule("com.example.a", thresholdSeconds = 3600),
            RuleEntity(type = "schedule", targetPackageOrCategory = "com.example.b", threshold = null, windowDefinition = null)
        )
        val result = InsightsMetrics.limitCompliance(rules, mapOf("com.example.a" to 100L))
        assertEquals(1, result.total)
    }

    @Test
    fun `limitCompliance with no limit rules reports zero total`() {
        val result = InsightsMetrics.limitCompliance(emptyList(), emptyMap())
        assertEquals(0, result.total)
        assertEquals(0, result.compliant)
    }

    private val zone = ZoneOffset.UTC
    private fun row(packageName: String, epochDay: Long, seconds: Long, label: String? = null) =
        DayAppUsageRow(packageName = packageName, label = label, epochDay = epochDay, totalUsageSeconds = seconds)

    @Test
    fun `weeklyBreakdown splits trailing days into 7-day buckets, most recent first`() {
        val today = 100L
        val rows = listOf(row("a", epochDay = 100, seconds = 60), row("a", epochDay = 93, seconds = 120))
        val weeks = InsightsMetrics.weeklyBreakdown(rows, emptyList(), emptyList(), today, weeks = 2, zoneId = zone)
        assertEquals(2, weeks.size)
        assertEquals(94L, weeks[0].startEpochDay)
        assertEquals(100L, weeks[0].endEpochDayInclusive)
        assertEquals(60L, weeks[0].totalUsageSeconds)
        assertEquals(87L, weeks[1].startEpochDay)
        assertEquals(93L, weeks[1].endEpochDayInclusive)
        assertEquals(120L, weeks[1].totalUsageSeconds)
    }

    @Test
    fun `weeklyBreakdown excludes rows outside the requested window entirely`() {
        val today = 100L
        val rows = listOf(row("a", epochDay = 100, seconds = 60), row("a", epochDay = 50, seconds = 999))
        val weeks = InsightsMetrics.weeklyBreakdown(rows, emptyList(), emptyList(), today, weeks = 1, zoneId = zone)
        assertEquals(60L, weeks[0].totalUsageSeconds)
    }

    @Test
    fun `weeklyBreakdown ranks topApps by total seconds within the week, capped at three`() {
        val today = 100L
        val rows = listOf(
            row("a", 100, 500), row("b", 100, 900), row("c", 100, 300), row("d", 100, 100)
        )
        val weeks = InsightsMetrics.weeklyBreakdown(rows, emptyList(), emptyList(), today, weeks = 1, zoneId = zone)
        assertEquals(listOf("b" to 900L, "a" to 500L, "c" to 300L), weeks[0].topApps)
    }

    @Test
    fun `weeklyBreakdown reports zero ruleDays when no limit rule ever has usage that week`() {
        val today = 100L
        val rules = listOf(limitRule("com.example.a", thresholdSeconds = 60))
        val rows = listOf(row("com.example.other", 100, 30))
        val weeks = InsightsMetrics.weeklyBreakdown(rows, rules, emptyList(), today, weeks = 1, zoneId = zone)
        assertEquals(0, weeks[0].ruleDays)
        assertEquals(0, weeks[0].compliantDays)
    }

    @Test
    fun `weeklyBreakdown counts a day compliant only when every limit rule with usage that day is under threshold`() {
        val today = 101L
        val rules = listOf(limitRule("com.example.a", thresholdSeconds = 60))
        val rows = listOf(
            row("com.example.a", 100, 30), // compliant day
            row("com.example.a", 101, 90)  // over-limit day
        )
        val weeks = InsightsMetrics.weeklyBreakdown(rows, rules, emptyList(), today, weeks = 1, zoneId = zone)
        assertEquals(2, weeks[0].ruleDays)
        assertEquals(1, weeks[0].compliantDays)
    }

    @Test
    fun `weeklyBreakdown sums focus session completed minutes within each week's real millis bounds`() {
        val today = 100L
        val session = focusSession(startTs = 0, completedMinutes = 15)
        val weeks = InsightsMetrics.weeklyBreakdown(emptyList(), emptyList(), listOf(session), today, weeks = 1, zoneId = zone)
        // The session started at epoch millis 0 (1970-01-01), long before epochDay 94..100 — outside this week's window.
        assertEquals(0L, weeks[0].focusSeconds)
    }

    @Test
    fun `highlight reports a better-week note when this week's total usage is lower than last week's`() {
        val thisWeek = InsightsMetrics.WeekSummary(0, 6, totalUsageSeconds = 1000, focusSeconds = 0, compliantDays = 0, ruleDays = 0, topApps = emptyList())
        val lastWeek = thisWeek.copy(totalUsageSeconds = 2000)
        assertEquals("🌱 Better week — usage is down from last week.", InsightsMetrics.highlight(thisWeek, lastWeek))
    }

    @Test
    fun `highlight reports consistency when every limit was met and usage isn't down`() {
        val thisWeek = InsightsMetrics.WeekSummary(0, 6, totalUsageSeconds = 2000, focusSeconds = 0, compliantDays = 5, ruleDays = 5, topApps = emptyList())
        val lastWeek = thisWeek.copy(totalUsageSeconds = 1000)
        assertEquals("🔥 Strong consistency — every limit met this week.", InsightsMetrics.highlight(thisWeek, lastWeek))
    }

    @Test
    fun `highlight is null when there is nothing notable to report`() {
        val thisWeek = InsightsMetrics.WeekSummary(0, 6, totalUsageSeconds = 2000, focusSeconds = 0, compliantDays = 2, ruleDays = 5, topApps = emptyList())
        val lastWeek = thisWeek.copy(totalUsageSeconds = 1000)
        assertNull(InsightsMetrics.highlight(thisWeek, lastWeek))
    }

    @Test
    fun `highlight is null with no previous week and nothing notable`() {
        val thisWeek = InsightsMetrics.WeekSummary(0, 6, totalUsageSeconds = 2000, focusSeconds = 0, compliantDays = 0, ruleDays = 0, topApps = emptyList())
        assertNull(InsightsMetrics.highlight(thisWeek, null))
    }
}

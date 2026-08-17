package com.orlune.app.core.domain.insights

import com.orlune.app.data.local.entity.FocusSessionEntity
import com.orlune.app.data.local.entity.RuleEntity
import org.junit.Assert.assertEquals
import org.junit.Test

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
}

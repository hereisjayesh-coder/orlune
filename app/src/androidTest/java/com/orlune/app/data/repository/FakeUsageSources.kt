package com.orlune.app.data.repository

import com.orlune.app.core.domain.usage.RawUsageEvent
import com.orlune.app.platform.usage.AppLabelSource
import com.orlune.app.platform.usage.UsageEventSource

/**
 * Filters by [startTime, endTime) like the real UsageStatsManager-backed reader does —
 * this is what makes the watermark/duplicate-processing behavior actually testable
 * against a fake instead of real, non-deterministic device usage data.
 */
class FakeUsageEventSource(private val allEvents: List<RawUsageEvent>) : UsageEventSource {
    override fun queryEvents(startTime: Long, endTime: Long): List<RawUsageEvent> =
        allEvents.filter { it.timestamp >= startTime && it.timestamp < endTime }
            .sortedBy { it.timestamp }
}

class FakeAppLabelSource(private val labels: Map<String, String> = emptyMap()) : AppLabelSource {
    override fun resolveLabel(packageName: String): String = labels[packageName] ?: packageName
}

package com.orlune.app.platform.usage

import com.orlune.app.core.domain.usage.RawUsageEvent

/**
 * Contract for [UsageRepository][com.orlune.app.data.repository.UsageRepository]'s
 * event source. Exists so instrumentation tests can substitute a fake instead of
 * real `UsageStatsManager` data (which isn't controllable/deterministic in a test
 * environment) while still exercising the real Room database.
 */
interface UsageEventSource {
    fun queryEvents(startTime: Long, endTime: Long): List<RawUsageEvent>
}

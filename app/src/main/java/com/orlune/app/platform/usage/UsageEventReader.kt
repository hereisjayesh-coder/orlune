package com.orlune.app.platform.usage

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.orlune.app.core.domain.usage.RawUsageEvent
import com.orlune.app.core.domain.usage.UsageEventType

/**
 * Thin wrapper over `UsageStatsManager.queryEvents` — the only place `UsageEvents.Event`
 * constants are referenced. Maps to [RawUsageEvent] so the rest of the pipeline
 * ([com.orlune.app.core.domain.usage.SessionCalculator]) has no Android framework
 * dependency and stays plain-unit-testable.
 */
class UsageEventReader(private val context: Context) : UsageEventSource {

    /**
     * Events are returned in chronological order, as `UsageEvents` itself guarantees.
     *
     * Deliberately uses MOVE_TO_FOREGROUND/MOVE_TO_BACKGROUND (deprecated in favor of
     * ACTIVITY_RESUMED/ACTIVITY_PAUSED) rather than the replacement: the new events
     * are reported per-Activity, not per-app, so a single app with multiple internal
     * activities would emit multiple resume/pause pairs while never actually leaving
     * the foreground — that would fragment real sessions into many spurious short
     * ones. The deprecated constants report app-level UID transitions, which is what
     * SessionCalculator actually needs (`docs/phase-0-research.md` Section 9 names
     * these exact events), and they remain fully functional.
     */
    @Suppress("DEPRECATION")
    override fun queryEvents(startTime: Long, endTime: Long): List<RawUsageEvent> {
        if (endTime <= startTime) return emptyList()

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val events = usageStatsManager.queryEvents(startTime, endTime)
        val result = mutableListOf<RawUsageEvent>()
        val event = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val type = when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> UsageEventType.FOREGROUND
                UsageEvents.Event.MOVE_TO_BACKGROUND -> UsageEventType.BACKGROUND
                UsageEvents.Event.DEVICE_SHUTDOWN -> UsageEventType.DEVICE_SHUTDOWN
                else -> UsageEventType.OTHER
            }
            result += RawUsageEvent(event.packageName, type, event.timeStamp)
        }
        return result
    }
}

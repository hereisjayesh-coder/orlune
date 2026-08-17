package com.orlune.app.data.repository

import com.orlune.app.core.domain.usage.ClosedSession
import com.orlune.app.core.domain.usage.SessionCalculator
import com.orlune.app.core.domain.usage.UsageAggregator
import com.orlune.app.data.local.dao.AppDailyUsage
import com.orlune.app.data.local.dao.AppDao
import com.orlune.app.data.local.dao.DailyUsageDao
import com.orlune.app.data.local.dao.SessionDao
import com.orlune.app.data.local.dao.UserPreferenceDao
import com.orlune.app.data.local.entity.AppEntity
import com.orlune.app.data.local.entity.DailyUsageEntity
import com.orlune.app.data.local.entity.SessionEntity
import com.orlune.app.data.local.entity.UserPreferenceEntity
import com.orlune.app.platform.usage.AppLabelSource
import com.orlune.app.platform.usage.UsageEventSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Owns the platform-events -> Room pipeline (Android Usage API -> event processing
 * -> aggregation -> database), per `docs/phase-0-research.md` Section 1's
 * architecture diagram. Everything here runs on a background dispatcher — callers
 * (the worker, the debug screen) are responsible for that, this class doesn't switch
 * dispatchers itself.
 */
class UsageRepository(
    private val usageEventSource: UsageEventSource,
    private val appLabelSource: AppLabelSource,
    private val appDao: AppDao,
    private val sessionDao: SessionDao,
    private val dailyUsageDao: DailyUsageDao,
    private val userPreferenceDao: UserPreferenceDao,
    private val sessionCalculator: SessionCalculator = SessionCalculator(),
    private val zoneId: ZoneId = ZoneId.systemDefault(),
    private val nowMillis: () -> Long = System::currentTimeMillis
) {

    fun observeTodayUsage(): Flow<List<AppDailyUsage>> =
        dailyUsageDao.observeForDayWithLabels(todayEpochDay())

    /**
     * Reads new events since the last processed watermark, turns them into sessions,
     * aggregates affected days, and persists all of it. Safe to call repeatedly (from
     * the periodic worker or a manual "refresh" action) — idempotent via the
     * watermark in [UserPreferenceEntity], and duplicate-safe because closed/open
     * sessions are reconciled against what's already in the database rather than
     * blindly inserted.
     */
    suspend fun processNewEvents() {
        val now = nowMillis()
        val watermark = readWatermark()
        // +1 so the event exactly at the watermark (already processed last run) isn't
        // re-read — the actual source of "duplicate processing" safety.
        val startTime = watermark?.plus(1) ?: startOfToday()
        if (startTime >= now) return

        val events = usageEventSource.queryEvents(startTime, now)
        val existingOpenEntities = sessionDao.getOpenSessions()
        val existingOpenByPackage = existingOpenEntities.associateBy { it.packageName }

        val result = sessionCalculator.process(
            events = events,
            existingOpenSessions = existingOpenByPackage.mapValues { it.value.startTs }
        )

        persistClosedSessions(result.closedSessions, existingOpenByPackage)
        persistStillOpenSessions(result.stillOpen, existingOpenByPackage)
        ensureAppsKnown(result.closedSessions.map { it.packageName } + result.stillOpen.keys)

        val touchedDays = result.closedSessions.map { it.packageName to epochDayOf(it.startTs) }.distinct()
        for ((packageName, epochDay) in touchedDays) {
            recomputeDailyUsage(packageName, epochDay)
        }

        if (events.isNotEmpty()) {
            writeWatermark(events.maxOf { it.timestamp })
        }
    }

    private suspend fun persistClosedSessions(
        closedSessions: List<ClosedSession>,
        existingOpenByPackage: Map<String, SessionEntity>
    ) {
        val byPackage = closedSessions.groupBy { it.packageName }
        for ((packageName, segments) in byPackage) {
            val sorted = segments.sortedBy { it.startTs }
            val existingEntity = existingOpenByPackage[packageName]
            sorted.forEachIndexed { index, segment ->
                val reuseId = if (index == 0 && existingEntity != null && existingEntity.startTs == segment.startTs) {
                    existingEntity.id
                } else {
                    0L
                }
                sessionDao.upsert(
                    SessionEntity(
                        id = reuseId,
                        packageName = segment.packageName,
                        startTs = segment.startTs,
                        endTs = segment.endTs
                    )
                )
            }
        }
    }

    private suspend fun persistStillOpenSessions(
        stillOpen: Map<String, Long>,
        existingOpenByPackage: Map<String, SessionEntity>
    ) {
        for ((packageName, startTs) in stillOpen) {
            val existingEntity = existingOpenByPackage[packageName]
            if (existingEntity == null || existingEntity.startTs != startTs) {
                sessionDao.upsert(SessionEntity(packageName = packageName, startTs = startTs, endTs = null))
            }
        }
    }

    private suspend fun ensureAppsKnown(packageNames: Collection<String>) {
        for (packageName in packageNames.distinct()) {
            if (appDao.get(packageName) == null) {
                val label = appLabelSource.resolveLabel(packageName)
                appDao.upsert(AppEntity(packageName = packageName, label = label, category = null, isEssential = false))
            }
        }
    }

    private suspend fun recomputeDailyUsage(packageName: String, epochDay: Long) {
        val dayStart = epochDayStartMillis(epochDay)
        val dayEnd = epochDayStartMillis(epochDay + 1)
        val daySessions = sessionDao.getClosedSessionsForDay(packageName, dayStart, dayEnd)
        val totals = UsageAggregator.aggregate(
            daySessions.map { ClosedSession(it.packageName, it.startTs, it.endTs!!) }
        )
        val existing = dailyUsageDao.getForAppAndDay(packageName, epochDay)
        dailyUsageDao.upsert(
            DailyUsageEntity(
                id = existing?.id ?: 0,
                packageName = packageName,
                epochDay = epochDay,
                totalUsageSeconds = totals.totalUsageSeconds,
                launchCount = totals.launchCount,
                sessionCount = totals.sessionCount
            )
        )
    }

    private suspend fun readWatermark(): Long? =
        userPreferenceDao.observe(WATERMARK_KEY).first()?.value?.toLongOrNull()

    private suspend fun writeWatermark(timestamp: Long) {
        userPreferenceDao.upsert(UserPreferenceEntity(WATERMARK_KEY, timestamp.toString()))
    }

    private fun epochDayOf(timestampMillis: Long): Long =
        Instant.ofEpochMilli(timestampMillis).atZone(zoneId).toLocalDate().toEpochDay()

    private fun todayEpochDay(): Long = epochDayOf(nowMillis())

    // Derived from nowMillis()/zoneId (the same injectable clock as everything else
    // in this class), not LocalDate.now() — that would read the real system clock
    // even when nowMillis() is faked for a test, silently ignoring the injected time.
    private fun startOfToday(): Long = epochDayStartMillis(todayEpochDay())

    private fun epochDayStartMillis(epochDay: Long): Long =
        LocalDate.ofEpochDay(epochDay).atStartOfDay(zoneId).toInstant().toEpochMilli()

    companion object {
        private const val WATERMARK_KEY = "usage.lastProcessedEventTime"
    }
}

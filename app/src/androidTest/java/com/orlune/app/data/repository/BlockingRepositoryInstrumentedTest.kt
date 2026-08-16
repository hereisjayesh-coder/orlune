package com.orlune.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.orlune.app.core.domain.rules.BlockDecision
import com.orlune.app.data.local.OrluneDatabase
import com.orlune.app.data.local.entity.AppListEntryEntity
import com.orlune.app.data.local.entity.DailyUsageEntity
import com.orlune.app.data.local.entity.RuleEntity
import com.orlune.app.data.local.entity.ScheduleEntity
import com.orlune.app.data.local.entity.SessionEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Exercises [BlockingRepository] against a real in-memory Room database, mirroring
 * UsageRepositoryInstrumentedTest's pattern. "Current foreground app" is a real open
 * [SessionEntity] row (`endTs = null`) rather than a faked live lookup — this is the
 * same source of truth [BlockingRepository] itself uses, so no fake is needed here.
 */
@RunWith(AndroidJUnit4::class)
class BlockingRepositoryInstrumentedTest {

    private lateinit var database: OrluneDatabase

    // UTC everywhere so day/time-boundary math doesn't depend on the test device's zone.
    private val zone = ZoneOffset.UTC

    private fun ts(dateTime: String): Long =
        LocalDateTime.parse(dateTime).atZone(zone).toInstant().toEpochMilli()

    private fun epochDayOf(timestampMillis: Long): Long =
        Instant.ofEpochMilli(timestampMillis).atZone(zone).toLocalDate().toEpochDay()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, OrluneDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    /** [openSessionStartTs] defaults to [now] — negligible elapsed-time contribution. */
    private suspend fun repository(foregroundPackage: String?, now: Long, openSessionStartTs: Long = now): BlockingRepository {
        if (foregroundPackage != null) {
            database.sessionDao().upsert(SessionEntity(packageName = foregroundPackage, startTs = openSessionStartTs, endTs = null))
        }
        return BlockingRepository(
            ruleDao = database.ruleDao(),
            scheduleDao = database.scheduleDao(),
            appListEntryDao = database.appListEntryDao(),
            dailyUsageDao = database.dailyUsageDao(),
            sessionDao = database.sessionDao(),
            ownPackageName = "com.orlune.app",
            zoneId = zone,
            nowMillis = { now }
        )
    }

    @Test
    fun evaluate_blocksWhenRealPersistedUsageExceedsRealPersistedLimit() = runTest {
        val now = ts("2026-08-16T12:00:00")
        database.dailyUsageDao().upsert(
            DailyUsageEntity(packageName = "app.a", epochDay = epochDayOf(now), totalUsageSeconds = 4000, launchCount = 1, sessionCount = 1)
        )
        database.ruleDao().upsert(RuleEntity(type = "limit", targetPackageOrCategory = "app.a", threshold = 3600, windowDefinition = null))

        val outcome = repository("app.a", now).evaluate()

        assertEquals(BlockDecision.BLOCK, outcome.decision)
    }

    @Test
    fun evaluate_allowsWhenRealPersistedUsageIsUnderLimit() = runTest {
        val now = ts("2026-08-16T12:00:00")
        database.dailyUsageDao().upsert(
            DailyUsageEntity(packageName = "app.a", epochDay = epochDayOf(now), totalUsageSeconds = 100, launchCount = 1, sessionCount = 1)
        )
        database.ruleDao().upsert(RuleEntity(type = "limit", targetPackageOrCategory = "app.a", threshold = 3600, windowDefinition = null))

        val outcome = repository("app.a", now).evaluate()

        assertEquals(BlockDecision.ALLOW, outcome.decision)
    }

    @Test
    fun evaluate_blocksWhenARealOpenSessionAloneExceedsTheLimit() = runTest {
        val now = ts("2026-08-16T12:00:00")
        database.ruleDao().upsert(RuleEntity(type = "limit", targetPackageOrCategory = "app.a", threshold = 60, windowDefinition = null))

        // No closed/aggregated usage at all — only a real open session running 90s.
        val outcome = repository("app.a", now, openSessionStartTs = now - 90_000).evaluate()

        assertEquals(BlockDecision.BLOCK, outcome.decision)
    }

    @Test
    fun evaluate_blocksForARealScheduleRowJoinedByForeignKey() = runTest {
        // 2026-08-17 is a Monday.
        val now = ts("2026-08-17T12:00:00")
        val ruleId = database.ruleDao().upsert(
            RuleEntity(type = "schedule", targetPackageOrCategory = "app.a", threshold = null, windowDefinition = null)
        )
        database.scheduleDao().upsert(
            ScheduleEntity(daysOfWeek = "MON", startTime = "09:00", endTime = "17:00", associatedRuleId = ruleId)
        )

        val outcome = repository("app.a", now).evaluate()

        assertEquals(BlockDecision.BLOCK, outcome.decision)
    }

    @Test
    fun evaluate_allowListOverridesARealTriggeredRule() = runTest {
        val now = ts("2026-08-16T12:00:00")
        database.dailyUsageDao().upsert(
            DailyUsageEntity(packageName = "app.a", epochDay = epochDayOf(now), totalUsageSeconds = 4000, launchCount = 1, sessionCount = 1)
        )
        database.ruleDao().upsert(RuleEntity(type = "limit", targetPackageOrCategory = "app.a", threshold = 3600, windowDefinition = null))
        database.appListEntryDao().upsert(AppListEntryEntity(packageName = "app.a", listType = "allow"))

        val outcome = repository("app.a", now).evaluate()

        assertEquals(BlockDecision.ALLOW, outcome.decision)
    }

    @Test
    fun evaluate_noOpenSessionAllowsAndDoesNotCrash() = runTest {
        val outcome = repository(null, ts("2026-08-16T12:00:00")).evaluate()

        assertEquals(BlockDecision.ALLOW, outcome.decision)
    }
}

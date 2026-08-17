package com.orlune.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.orlune.app.core.domain.usage.RawUsageEvent
import com.orlune.app.core.domain.usage.SessionCalculator
import com.orlune.app.core.domain.usage.UsageEventType
import com.orlune.app.data.local.OrluneDatabase
import com.orlune.app.data.local.entity.UserPreferenceEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Exercises the real pipeline (Android Usage API -> event processing -> aggregation
 * -> database) against a real in-memory Room database, with only the two
 * Android-platform-touching collaborators faked (see FakeUsageSources.kt) — usage
 * events aren't controllable/deterministic on a real test device.
 */
@RunWith(AndroidJUnit4::class)
class UsageRepositoryInstrumentedTest {

    private lateinit var database: OrluneDatabase

    // UTC everywhere so day-boundary math doesn't depend on the test device's zone.
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

    private fun repositoryWith(events: List<RawUsageEvent>, now: Long, zoneId: ZoneId = zone): UsageRepository =
        UsageRepository(
            usageEventSource = FakeUsageEventSource(events),
            appLabelSource = FakeAppLabelSource(mapOf("app.a" to "App A")),
            appDao = database.appDao(),
            sessionDao = database.sessionDao(),
            dailyUsageDao = database.dailyUsageDao(),
            userPreferenceDao = database.userPreferenceDao(),
            sessionCalculator = SessionCalculator(zoneId),
            zoneId = zoneId,
            nowMillis = { now }
        )

    @Test
    fun processNewEvents_writesSessionAndDailyUsageAndAppRows() = runTest {
        val now = ts("2026-08-16T12:00:00")
        val events = listOf(
            RawUsageEvent("app.a", UsageEventType.FOREGROUND, ts("2026-08-16T10:00:00")),
            RawUsageEvent("app.a", UsageEventType.BACKGROUND, ts("2026-08-16T10:05:00"))
        )

        repositoryWith(events, now).processNewEvents()

        val sessions = database.sessionDao().observeForApp("app.a").first()
        assertEquals(1, sessions.size)
        assertEquals(ts("2026-08-16T10:00:00"), sessions[0].startTs)
        assertEquals(ts("2026-08-16T10:05:00"), sessions[0].endTs)

        val app = database.appDao().get("app.a")
        assertEquals("App A", app?.label)

        val today = database.dailyUsageDao().observeForDayWithLabels(epochDayOf(now)).first()
        assertEquals(1, today.size)
        assertEquals(300L, today[0].totalUsageSeconds) // 5 minutes
        assertEquals("App A", today[0].label)
    }

    @Test
    fun processNewEvents_isIdempotent_doesNotDuplicateOnRepeatedCalls() = runTest {
        val now = ts("2026-08-16T12:00:00")
        val events = listOf(
            RawUsageEvent("app.a", UsageEventType.FOREGROUND, ts("2026-08-16T10:00:00")),
            RawUsageEvent("app.a", UsageEventType.BACKGROUND, ts("2026-08-16T10:05:00"))
        )
        val repository = repositoryWith(events, now)

        repository.processNewEvents()
        repository.processNewEvents() // same fixed event list, called again

        val sessions = database.sessionDao().observeForApp("app.a").first()
        assertEquals(1, sessions.size) // not duplicated

        val today = database.dailyUsageDao().observeForDay(epochDayOf(now)).first()
        assertEquals(1, today.size)
        assertEquals(300L, today[0].totalUsageSeconds) // unchanged, not doubled
    }

    @Test
    fun processNewEvents_carriesOpenSessionAcrossRunsThenClosesIt() = runTest {
        val firstNow = ts("2026-08-16T10:02:00")
        val openEvent = listOf(
            RawUsageEvent("app.a", UsageEventType.FOREGROUND, ts("2026-08-16T10:00:00"))
        )
        repositoryWith(openEvent, firstNow).processNewEvents()

        val openSessions = database.sessionDao().getOpenSessions()
        assertEquals(1, openSessions.size)
        assertNull(openSessions[0].endTs)
        assertEquals(ts("2026-08-16T10:00:00"), openSessions[0].startTs)

        // Only closed sessions are aggregated — nothing should exist yet.
        assertTrue(database.dailyUsageDao().observeForDay(epochDayOf(firstNow)).first().isEmpty())

        val secondNow = ts("2026-08-16T10:10:00")
        val closeEvent = listOf(
            RawUsageEvent("app.a", UsageEventType.BACKGROUND, ts("2026-08-16T10:08:00"))
        )
        repositoryWith(closeEvent, secondNow).processNewEvents()

        val sessionsAfterClose = database.sessionDao().observeForApp("app.a").first()
        assertEquals(1, sessionsAfterClose.size) // updated in place, not a second row
        assertEquals(ts("2026-08-16T10:00:00"), sessionsAfterClose[0].startTs)
        assertEquals(ts("2026-08-16T10:08:00"), sessionsAfterClose[0].endTs)

        val today = database.dailyUsageDao().observeForDay(epochDayOf(secondNow)).first()
        assertEquals(1, today.size)
        assertEquals(480L, today[0].totalUsageSeconds) // 8 minutes
    }

    // Regression coverage for a bug where UsageRepository.startOfToday() called
    // LocalDate.now() — the real system clock — instead of deriving "today" from the
    // injected nowMillis()/zoneId clock every other date computation in the class
    // uses. That silently broke first-run processing (before any watermark exists)
    // whenever the real device date didn't match a test's assumed "now".

    @Test
    fun processNewEvents_firstRun_usesInjectedClock_notRealSystemClock() = runTest {
        // A date deliberately far from any real calendar date this test could
        // actually execute on. If startOfToday() ever again reads the real system
        // clock instead of nowMillis(), this event (which only exists "today"
        // relative to the fake now) would fall outside the derived query window and
        // no session would be written — exactly how the original bug manifested.
        val now = ts("2099-03-10T12:00:00")
        val events = listOf(
            RawUsageEvent("app.a", UsageEventType.FOREGROUND, ts("2099-03-10T10:00:00")),
            RawUsageEvent("app.a", UsageEventType.BACKGROUND, ts("2099-03-10T10:05:00"))
        )

        repositoryWith(events, now).processNewEvents()

        val sessions = database.sessionDao().observeForApp("app.a").first()
        assertEquals(1, sessions.size)

        val today = database.dailyUsageDao().observeForDay(epochDayOf(now)).first()
        assertEquals(1, today.size)
        assertEquals(300L, today[0].totalUsageSeconds) // 5 minutes
    }

    @Test
    fun processNewEvents_firstRun_respectsConfiguredNonUtcZone() = runTest {
        val tokyoZone = ZoneOffset.ofHours(9)
        fun tsTokyo(dateTime: String) = LocalDateTime.parse(dateTime).atZone(tokyoZone).toInstant().toEpochMilli()

        // Local midnight in UTC+9 is 15:00 the previous calendar day in UTC. If the
        // day boundary were ever computed against UTC (or the test JVM's default
        // zone) instead of the repository's configured zoneId, this event — placed
        // just after local midnight — would land on the wrong side of "today".
        val now = tsTokyo("2026-08-16T09:00:00")
        val events = listOf(
            RawUsageEvent("app.a", UsageEventType.FOREGROUND, tsTokyo("2026-08-16T00:00:30")),
            RawUsageEvent("app.a", UsageEventType.BACKGROUND, tsTokyo("2026-08-16T00:05:30"))
        )

        repositoryWith(events, now, tokyoZone).processNewEvents()

        val sessions = database.sessionDao().observeForApp("app.a").first()
        assertEquals(1, sessions.size)

        val todayLocalEpochDay = Instant.ofEpochMilli(now).atZone(tokyoZone).toLocalDate().toEpochDay()
        val today = database.dailyUsageDao().observeForDay(todayLocalEpochDay).first()
        assertEquals(1, today.size)
        assertEquals(300L, today[0].totalUsageSeconds) // 5 minutes
    }

    @Test
    fun processNewEvents_firstRun_dayBoundaryIsExactlyLocalMidnight() = runTest {
        val now = ts("2026-08-16T00:30:00")
        val events = listOf(
            // Entirely before midnight — must NOT be picked up by a first run whose
            // derived start-of-day is exactly 2026-08-16T00:00:00.
            RawUsageEvent("app.before", UsageEventType.FOREGROUND, ts("2026-08-15T23:59:00")),
            RawUsageEvent("app.before", UsageEventType.BACKGROUND, ts("2026-08-15T23:59:30")),
            // Starts exactly at local midnight — must be included (inclusive lower bound).
            RawUsageEvent("app.after", UsageEventType.FOREGROUND, ts("2026-08-16T00:00:00")),
            RawUsageEvent("app.after", UsageEventType.BACKGROUND, ts("2026-08-16T00:00:30"))
        )

        repositoryWith(events, now).processNewEvents()

        assertTrue(database.sessionDao().observeForApp("app.before").first().isEmpty())
        val afterSessions = database.sessionDao().observeForApp("app.after").first()
        assertEquals(1, afterSessions.size)
        assertEquals(ts("2026-08-16T00:00:00"), afterSessions[0].startTs)
    }

    @Test
    fun processNewEvents_sessionSpanningMidnight_splitsUsageAcrossBothDayBuckets() = runTest {
        // Pre-seed the watermark (mirrors UsageRepository's private WATERMARK_KEY) so
        // this run isn't a "first run" — the session under test starts before the
        // query window's day boundary and must still be read in full.
        database.userPreferenceDao().upsert(
            UserPreferenceEntity("usage.lastProcessedEventTime", ts("2026-08-15T23:00:00").toString())
        )

        val now = ts("2026-08-16T01:00:00")
        val events = listOf(
            RawUsageEvent("app.a", UsageEventType.FOREGROUND, ts("2026-08-15T23:50:00")),
            RawUsageEvent("app.a", UsageEventType.BACKGROUND, ts("2026-08-16T00:10:00"))
        )

        repositoryWith(events, now).processNewEvents()

        // SessionCalculator splits the 23:50->00:10 session at the midnight boundary;
        // UsageRepository.recomputeDailyUsage must bucket each half into its own day
        // using the same zoneId-derived day-start/day-end as everywhere else.
        val dayOne = database.dailyUsageDao().observeForDay(epochDayOf(ts("2026-08-15T23:50:00"))).first()
        assertEquals(1, dayOne.size)
        assertEquals(600L, dayOne[0].totalUsageSeconds) // 23:50 -> 00:00, 10 minutes

        val dayTwo = database.dailyUsageDao().observeForDay(epochDayOf(ts("2026-08-16T00:10:00"))).first()
        assertEquals(1, dayTwo.size)
        assertEquals(600L, dayTwo[0].totalUsageSeconds) // 00:00 -> 00:10, 10 minutes
    }
}

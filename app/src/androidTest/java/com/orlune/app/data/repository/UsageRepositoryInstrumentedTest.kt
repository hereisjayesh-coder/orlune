package com.orlune.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.orlune.app.core.domain.usage.RawUsageEvent
import com.orlune.app.core.domain.usage.SessionCalculator
import com.orlune.app.core.domain.usage.UsageEventType
import com.orlune.app.data.local.OrluneDatabase
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

    private fun repositoryWith(events: List<RawUsageEvent>, now: Long): UsageRepository =
        UsageRepository(
            usageEventSource = FakeUsageEventSource(events),
            appLabelSource = FakeAppLabelSource(mapOf("app.a" to "App A")),
            appDao = database.appDao(),
            sessionDao = database.sessionDao(),
            dailyUsageDao = database.dailyUsageDao(),
            userPreferenceDao = database.userPreferenceDao(),
            sessionCalculator = SessionCalculator(zone),
            zoneId = zone,
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
}

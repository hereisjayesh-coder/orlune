package com.orlune.app.data.repository

import com.orlune.app.core.domain.rules.BlockDecision
import com.orlune.app.data.local.dao.AppDailyUsage
import com.orlune.app.data.local.dao.AppPeriodUsage
import com.orlune.app.data.local.dao.AppListEntryDao
import com.orlune.app.data.local.dao.DailyUsageDao
import com.orlune.app.data.local.dao.FocusSessionDao
import com.orlune.app.data.local.dao.RuleDao
import com.orlune.app.data.local.dao.ScheduleDao
import com.orlune.app.data.local.dao.SessionDao
import com.orlune.app.data.local.entity.AppListEntryEntity
import com.orlune.app.data.local.entity.DailyUsageEntity
import com.orlune.app.data.local.entity.FocusSessionEntity
import com.orlune.app.data.local.entity.RuleEntity
import com.orlune.app.data.local.entity.ScheduleEntity
import com.orlune.app.data.local.entity.SessionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class BlockingRepositoryTest {

    private val zone = ZoneOffset.UTC

    /** 2024-01-01T12:00:00Z is a known Monday, noon. */
    private val fixedNow = Instant.parse("2024-01-01T12:00:00Z").toEpochMilli()

    private class FakeRuleDao(private val rules: List<RuleEntity>) : RuleDao {
        override suspend fun upsert(rule: RuleEntity): Long = 0
        override suspend fun delete(rule: RuleEntity) {}
        override fun observeAll(): Flow<List<RuleEntity>> = flowOf(rules)
    }

    private class FakeScheduleDao(private val schedulesByRule: Map<Long, List<ScheduleEntity>>) : ScheduleDao {
        override suspend fun upsert(schedule: ScheduleEntity): Long = 0
        override suspend fun delete(schedule: ScheduleEntity) {}
        override fun observeForRule(ruleId: Long): Flow<List<ScheduleEntity>> =
            flowOf(schedulesByRule[ruleId].orEmpty())
    }

    private class FakeAppListEntryDao(private val entries: List<AppListEntryEntity>) : AppListEntryDao {
        override suspend fun upsert(entry: AppListEntryEntity) {}
        override suspend fun delete(entry: AppListEntryEntity) {}
        override fun observeByType(listType: String): Flow<List<AppListEntryEntity>> =
            flowOf(entries.filter { it.listType == listType })
    }

    private class FakeDailyUsageDao(private val usageByPackage: Map<String, Long>) : DailyUsageDao {
        override suspend fun upsert(usage: DailyUsageEntity): Long = 0
        override suspend fun delete(usage: DailyUsageEntity) {}
        override fun observeForApp(packageName: String): Flow<List<DailyUsageEntity>> = flowOf(emptyList())
        override fun observeForDay(epochDay: Long): Flow<List<DailyUsageEntity>> = flowOf(emptyList())
        override suspend fun getForAppAndDay(packageName: String, epochDay: Long): DailyUsageEntity? {
            val seconds = usageByPackage[packageName] ?: return null
            return DailyUsageEntity(
                packageName = packageName,
                epochDay = epochDay,
                totalUsageSeconds = seconds,
                launchCount = 0,
                sessionCount = 0
            )
        }
        override fun observeForDayWithLabels(epochDay: Long): Flow<List<AppDailyUsage>> = flowOf(emptyList())
        override fun observeTotalSecondsBetween(startEpochDay: Long, endEpochDay: Long): Flow<Long> = flowOf(0L)
        override fun observeAppTotalsBetween(startEpochDay: Long, endEpochDay: Long): Flow<List<AppPeriodUsage>> = flowOf(emptyList())
        override fun observeCount(): Flow<Int> = flowOf(0)
    }

    private class FakeSessionDao(private val openSessions: List<SessionEntity>) : SessionDao {
        override suspend fun upsert(session: SessionEntity): Long = 0
        override suspend fun delete(session: SessionEntity) {}
        override fun observeForApp(packageName: String): Flow<List<SessionEntity>> = flowOf(emptyList())
        override fun observeOpenSessions(): Flow<List<SessionEntity>> = flowOf(openSessions)
        override suspend fun getOpenSessions(): List<SessionEntity> = openSessions
        override suspend fun getClosedSessionsForDay(packageName: String, dayStartTs: Long, dayEndTs: Long): List<SessionEntity> =
            emptyList()
        override fun observeCount(): Flow<Int> = flowOf(openSessions.size)
        override fun observeLongestSessionBetween(startTs: Long, endTs: Long): Flow<SessionEntity?> = flowOf(null)
    }

    private class FakeFocusSessionDao(private val sessions: List<FocusSessionEntity>) : FocusSessionDao {
        override suspend fun upsert(session: FocusSessionEntity): Long = 0
        override suspend fun delete(session: FocusSessionEntity) {}
        override fun observeAll(): Flow<List<FocusSessionEntity>> = flowOf(sessions)
    }

    private fun focusSession(
        blockedPackages: List<String>,
        startTs: Long,
        plannedMinutes: Int = 25,
        endTs: Long? = null,
        completedMinutes: Int = 0
    ) = FocusSessionEntity(
        startTs = startTs,
        endTs = endTs,
        plannedMinutes = plannedMinutes,
        completedMinutes = completedMinutes,
        blockedCategoryIds = "",
        blockedPackages = blockedPackages.joinToString(",")
    )

    /** Defaults the open session's start to "now" — negligible elapsed-time contribution, so existing
     *  aggregated-usage-only tests aren't affected unless a test overrides [openSessionStartTs]. */
    private fun repository(
        foregroundPackage: String? = "app.a",
        openSessionStartTs: Long = fixedNow,
        rules: List<RuleEntity> = emptyList(),
        schedulesByRule: Map<Long, List<ScheduleEntity>> = emptyMap(),
        listEntries: List<AppListEntryEntity> = emptyList(),
        usageByPackage: Map<String, Long> = emptyMap(),
        focusSessions: List<FocusSessionEntity> = emptyList(),
        zoneId: ZoneId = zone,
        now: Long = fixedNow
    ) = BlockingRepository(
        ruleDao = FakeRuleDao(rules),
        scheduleDao = FakeScheduleDao(schedulesByRule),
        appListEntryDao = FakeAppListEntryDao(listEntries),
        dailyUsageDao = FakeDailyUsageDao(usageByPackage),
        sessionDao = FakeSessionDao(
            if (foregroundPackage == null) emptyList()
            else listOf(SessionEntity(packageName = foregroundPackage, startTs = openSessionStartTs, endTs = null))
        ),
        focusSessionDao = FakeFocusSessionDao(focusSessions),
        ownPackageName = "com.orlune.app",
        zoneId = zoneId,
        nowMillis = { now }
    )

    @Test
    fun `no rules allows`() = runTest {
        assertEquals(BlockDecision.ALLOW, repository().evaluate().decision)
    }

    @Test
    fun `limit rule under threshold allows`() = runTest {
        val rule = RuleEntity(id = 1, type = "limit", targetPackageOrCategory = "app.a", threshold = 3600, windowDefinition = null)
        val outcome = repository(rules = listOf(rule), usageByPackage = mapOf("app.a" to 1000)).evaluate()
        assertEquals(BlockDecision.ALLOW, outcome.decision)
    }

    @Test
    fun `limit rule at or over threshold blocks`() = runTest {
        val rule = RuleEntity(id = 1, type = "limit", targetPackageOrCategory = "app.a", threshold = 3600, windowDefinition = null)
        val outcome = repository(rules = listOf(rule), usageByPackage = mapOf("app.a" to 4000)).evaluate()
        assertEquals(BlockDecision.BLOCK, outcome.decision)
    }

    @Test
    fun `limit rule with null threshold does not block`() = runTest {
        val rule = RuleEntity(id = 1, type = "limit", targetPackageOrCategory = "app.a", threshold = null, windowDefinition = null)
        val outcome = repository(rules = listOf(rule), usageByPackage = mapOf("app.a" to 999_999)).evaluate()
        assertEquals(BlockDecision.ALLOW, outcome.decision)
    }

    @Test
    fun `a currently-open session's elapsed time counts toward the limit`() = runTest {
        val rule = RuleEntity(id = 1, type = "limit", targetPackageOrCategory = "app.a", threshold = 60, windowDefinition = null)
        // No closed-session usage yet, but the open session has been running 90s.
        val outcome = repository(rules = listOf(rule), openSessionStartTs = fixedNow - 90_000).evaluate()
        assertEquals(BlockDecision.BLOCK, outcome.decision)
    }

    @Test
    fun `a currently-open session under the limit does not block`() = runTest {
        val rule = RuleEntity(id = 1, type = "limit", targetPackageOrCategory = "app.a", threshold = 60, windowDefinition = null)
        val outcome = repository(rules = listOf(rule), openSessionStartTs = fixedNow - 10_000).evaluate()
        assertEquals(BlockDecision.ALLOW, outcome.decision)
    }

    @Test
    fun `open session elapsed time adds to aggregated closed-session usage`() = runTest {
        val rule = RuleEntity(id = 1, type = "limit", targetPackageOrCategory = "app.a", threshold = 60, windowDefinition = null)
        // 40s already aggregated + 30s open session = 70s, over the 60s threshold.
        val outcome = repository(
            rules = listOf(rule),
            usageByPackage = mapOf("app.a" to 40),
            openSessionStartTs = fixedNow - 30_000
        ).evaluate()
        assertEquals(BlockDecision.BLOCK, outcome.decision)
    }

    @Test
    fun `active schedule rule blocks`() = runTest {
        val rule = RuleEntity(id = 1, type = "schedule", targetPackageOrCategory = "app.a", threshold = null, windowDefinition = null)
        val schedule = ScheduleEntity(name = "Work hours", daysOfWeek = "MON", startTime = "09:00", endTime = "17:00", associatedRuleId = 1)
        val outcome = repository(rules = listOf(rule), schedulesByRule = mapOf(1L to listOf(schedule))).evaluate()
        assertEquals(BlockDecision.BLOCK, outcome.decision)
    }

    @Test
    fun `inactive schedule rule allows`() = runTest {
        val rule = RuleEntity(id = 1, type = "schedule", targetPackageOrCategory = "app.a", threshold = null, windowDefinition = null)
        val schedule = ScheduleEntity(name = "Evening", daysOfWeek = "MON", startTime = "22:00", endTime = "23:00", associatedRuleId = 1)
        val outcome = repository(rules = listOf(rule), schedulesByRule = mapOf(1L to listOf(schedule))).evaluate()
        assertEquals(BlockDecision.ALLOW, outcome.decision)
    }

    @Test
    fun `allow list overrides a triggered rule`() = runTest {
        val rule = RuleEntity(id = 1, type = "limit", targetPackageOrCategory = "app.a", threshold = 60, windowDefinition = null)
        val entry = AppListEntryEntity(packageName = "app.a", listType = "allow")
        val outcome = repository(
            rules = listOf(rule),
            listEntries = listOf(entry),
            usageByPackage = mapOf("app.a" to 100)
        ).evaluate()
        assertEquals(BlockDecision.ALLOW, outcome.decision)
    }

    @Test
    fun `block list overrides no triggered rule`() = runTest {
        val entry = AppListEntryEntity(packageName = "app.a", listType = "block")
        val outcome = repository(listEntries = listOf(entry)).evaluate()
        assertEquals(BlockDecision.BLOCK, outcome.decision)
    }

    @Test
    fun `own package is never blocked even with a matching rule`() = runTest {
        val rule = RuleEntity(id = 1, type = "limit", targetPackageOrCategory = "com.orlune.app", threshold = 1, windowDefinition = null)
        val outcome = repository(
            foregroundPackage = "com.orlune.app",
            rules = listOf(rule),
            usageByPackage = mapOf("com.orlune.app" to 999_999)
        ).evaluate()
        assertEquals(BlockDecision.ALLOW, outcome.decision)
    }

    @Test
    fun `no open session allows and does not crash`() = runTest {
        val outcome = repository(foregroundPackage = null).evaluate()
        assertEquals(BlockDecision.ALLOW, outcome.decision)
        assertEquals(null, outcome.packageName)
    }

    @Test
    fun `rules targeting a different package are ignored`() = runTest {
        val rule = RuleEntity(id = 1, type = "limit", targetPackageOrCategory = "app.other", threshold = 1, windowDefinition = null)
        val outcome = repository(rules = listOf(rule), usageByPackage = mapOf("app.a" to 999_999)).evaluate()
        assertEquals(BlockDecision.ALLOW, outcome.decision)
    }

    @Test
    fun `active focus session blocks a listed package`() = runTest {
        val session = focusSession(blockedPackages = listOf("app.a"), startTs = fixedNow - 60_000)
        val outcome = repository(focusSessions = listOf(session)).evaluate()
        assertEquals(BlockDecision.BLOCK, outcome.decision)
    }

    @Test
    fun `scheduled future focus session does not yet block`() = runTest {
        val session = focusSession(blockedPackages = listOf("app.a"), startTs = fixedNow + 60_000)
        val outcome = repository(focusSessions = listOf(session)).evaluate()
        assertEquals(BlockDecision.ALLOW, outcome.decision)
    }

    @Test
    fun `completed focus session does not block`() = runTest {
        val session = focusSession(
            blockedPackages = listOf("app.a"),
            startTs = fixedNow - 3_600_000,
            plannedMinutes = 25,
            endTs = fixedNow - 100_000,
            completedMinutes = 25
        )
        val outcome = repository(focusSessions = listOf(session)).evaluate()
        assertEquals(BlockDecision.ALLOW, outcome.decision)
    }

    @Test
    fun `interrupted focus session does not block`() = runTest {
        val session = focusSession(
            blockedPackages = listOf("app.a"),
            startTs = fixedNow - 3_600_000,
            plannedMinutes = 25,
            endTs = fixedNow - 100_000,
            completedMinutes = 5
        )
        val outcome = repository(focusSessions = listOf(session)).evaluate()
        assertEquals(BlockDecision.ALLOW, outcome.decision)
    }

    @Test
    fun `allow list overrides an active focus-session block`() = runTest {
        val session = focusSession(blockedPackages = listOf("app.a"), startTs = fixedNow - 60_000)
        val entry = AppListEntryEntity(packageName = "app.a", listType = "allow")
        val outcome = repository(focusSessions = listOf(session), listEntries = listOf(entry)).evaluate()
        assertEquals(BlockDecision.ALLOW, outcome.decision)
    }

    @Test
    fun `two concurrently-active focus sessions with different package lists both apply`() = runTest {
        val sessionForA = focusSession(blockedPackages = listOf("app.a"), startTs = fixedNow - 60_000)
        val sessionForOther = focusSession(blockedPackages = listOf("app.other"), startTs = fixedNow - 60_000)
        val outcome = repository(focusSessions = listOf(sessionForA, sessionForOther)).evaluate()
        assertEquals(BlockDecision.BLOCK, outcome.decision)
    }

    @Test
    fun `two overlapping schedule rules targeting the same package resolve deterministically`() = runTest {
        // Both active at fixedNow (Monday noon) -> still a single clean BLOCK, not a conflict.
        val ruleA = RuleEntity(id = 1, type = "schedule", targetPackageOrCategory = "app.a", threshold = null, windowDefinition = null)
        val ruleB = RuleEntity(id = 2, type = "schedule", targetPackageOrCategory = "app.a", threshold = null, windowDefinition = null)
        val scheduleA = ScheduleEntity(name = "Morning", daysOfWeek = "MON", startTime = "08:00", endTime = "14:00", associatedRuleId = 1)
        val scheduleB = ScheduleEntity(name = "Midday", daysOfWeek = "MON", startTime = "11:00", endTime = "17:00", associatedRuleId = 2)
        val outcome = repository(
            rules = listOf(ruleA, ruleB),
            schedulesByRule = mapOf(1L to listOf(scheduleA), 2L to listOf(scheduleB))
        ).evaluate()
        assertEquals(BlockDecision.BLOCK, outcome.decision)
    }

    @Test
    fun `schedule evaluation respects a non-UTC zone`() = runTest {
        // 2024-01-01T12:00:00Z is 2024-01-01T17:30 IST (UTC+5:30) -- still Monday, still inside 09:00-18:00.
        val kolkata = ZoneId.of("Asia/Kolkata")
        val rule = RuleEntity(id = 1, type = "schedule", targetPackageOrCategory = "app.a", threshold = null, windowDefinition = null)
        val schedule = ScheduleEntity(name = "IST work hours", daysOfWeek = "MON", startTime = "09:00", endTime = "18:00", associatedRuleId = 1)
        val outcome = repository(
            rules = listOf(rule),
            schedulesByRule = mapOf(1L to listOf(schedule)),
            zoneId = kolkata
        ).evaluate()
        assertEquals(BlockDecision.BLOCK, outcome.decision)
    }
}
